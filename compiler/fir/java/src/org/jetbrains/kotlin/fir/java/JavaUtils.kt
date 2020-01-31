/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirSourceElement
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance.*

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

internal val JavaClass.classKind: ClassKind
    get() = when {
        isAnnotationType -> ClassKind.ANNOTATION_CLASS
        isInterface -> ClassKind.INTERFACE
        isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

internal fun ClassId.toConeKotlinType(
    typeArguments: Array<ConeKotlinTypeProjection>,
    isNullable: Boolean
): ConeLookupTagBasedType {
    val lookupTag = ConeClassLikeLookupTagImpl(this)
    return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable)
}

internal fun FirTypeRef.toNotNullConeKotlinType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType =
    when (this) {
        is FirResolvedTypeRef -> type
        is FirJavaTypeRef -> {
            val javaType = type
            javaType.toNotNullConeKotlinType(session, javaTypeParameterStack)
        }
        else -> ConeKotlinErrorType("Unexpected type reference in JavaClassUseSiteMemberScope: ${this::class.java}")
    }

internal fun JavaType?.toNotNullConeKotlinType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType {
    return toConeKotlinTypeWithNullability(session, javaTypeParameterStack, ConeNullability.NOT_NULL)
}

internal fun JavaType.toFirJavaTypeRef(session: FirSession, javaTypeParameterStack: JavaTypeParameterStack): FirJavaTypeRef {
    val annotations = (this as? JavaClassifierType)?.annotations.orEmpty()
    return buildJavaTypeRef {
        annotations.mapTo(this.annotations) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
        type = this@toFirJavaTypeRef
    }
}

internal fun JavaClassifierType.toFirResolvedTypeRef(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    nullability: ConeNullability = ConeNullability.NOT_NULL,
    typeParametersNullability: ConeNullability = ConeNullability.NOT_NULL
): FirResolvedTypeRef {
    val coneType = this.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, nullability, typeParametersNullability)
    return buildResolvedTypeRef {
        type = coneType
        this@toFirResolvedTypeRef.annotations.mapTo(annotations) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
    }
}

internal fun JavaType?.toConeKotlinTypeWithNullability(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    nullability: ConeNullability,
    typeParametersNullability: ConeNullability = ConeNullability.NOT_NULL
): ConeKotlinType {
    return when (this) {
        is JavaClassifierType -> {
            toConeKotlinTypeWithNullability(session, nullability, typeParametersNullability, javaTypeParameterStack)
        }
        is JavaPrimitiveType -> {
            val primitiveType = type
            val kotlinPrimitiveName = when (val javaName = primitiveType?.typeName?.asString()) {
                null -> "Unit"
                else -> javaName.capitalize()
            }
            val classId = StandardClassIds.byName(kotlinPrimitiveName)
            classId.toConeKotlinType(emptyArray(), nullability.isNullable)
        }
        is JavaArrayType -> {
            val componentType = componentType
            if (componentType !is JavaPrimitiveType) {
                val classId = StandardClassIds.Array
                val argumentType = ConeFlexibleType(
                    componentType.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, ConeNullability.NOT_NULL),
                    componentType.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, ConeNullability.NULLABLE)
                )
                classId.toConeKotlinType(arrayOf(argumentType), nullability.isNullable)
            } else {
                val javaComponentName = componentType.type?.typeName?.asString()?.capitalize() ?: error("Array of voids")
                val classId = StandardClassIds.byName(javaComponentName + "Array")
                classId.toConeKotlinType(emptyArray(), nullability.isNullable)
            }
        }
        is JavaWildcardType -> bound?.toNotNullConeKotlinType(session, javaTypeParameterStack) ?: run {
            val classId = StandardClassIds.Any
            classId.toConeKotlinType(emptyArray(), nullability.isNullable)
        }
        null -> {
            val classId = StandardClassIds.Any
            classId.toConeKotlinType(emptyArray(), nullability.isNullable)
        }
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

internal fun JavaClassifierType.toConeKotlinTypeWithNullability(
    session: FirSession,
    nullability: ConeNullability,
    typeParametersNullability: ConeNullability,
    javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType {
    if (nullability == ConeNullability.UNKNOWN) {
        return ConeFlexibleType(
            toConeKotlinTypeWithNullability(session, ConeNullability.NOT_NULL, typeParametersNullability, javaTypeParameterStack),
            toConeKotlinTypeWithNullability(session, ConeNullability.NULLABLE, typeParametersNullability, javaTypeParameterStack)
        )
    }
    return when (val classifier = classifier) {
        is JavaClass -> {
            //val classId = classifier.classId!!
            var classId = JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!) ?: classifier.classId!!
            classId = classId.readOnlyToMutable() ?: classId

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            lookupTag.constructClassType(
                typeArguments.map { argument ->
                    argument.toConeProjection(
                        session, javaTypeParameterStack, boundTypeParameter = null, nullability = typeParametersNullability
                    )
                }.toTypedArray(), nullability.isNullable
            )
        }
        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            ConeTypeParameterTypeImpl(symbol.toLookupTag(), nullability.isNullable)
        }
        else -> ConeClassErrorType(reason = "Unexpected classifier: $classifier")
    }
}

internal fun JavaAnnotation.toFirAnnotationCall(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirAnnotationCall {
    return buildAnnotationCall {
        annotationTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(FirRegularClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false)
        }
        for (argument in this@toFirAnnotationCall.arguments) {
            arguments += argument.toFirExpression(session, javaTypeParameterStack)
        }
    }
}

@FirBuilderDsl
internal fun FirAnnotationContainerBuilder.addAnnotationsFrom(
    session: FirSession, javaAnnotationOwner: JavaAnnotationOwner, javaTypeParameterStack: JavaTypeParameterStack
) {
    for (annotation in javaAnnotationOwner.annotations) {
        annotations += annotation.toFirAnnotationCall(session, javaTypeParameterStack)
    }
}

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession, index: Int, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return buildJavaValueParameter {
        source = (this@toFirValueParameter as? JavaElementImpl<*>)?.psi?.toFirSourceElement()
        this.session = session
        name = this@toFirValueParameter.name ?: Name.identifier("p$index")
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack)
        isVararg = this@toFirValueParameter.isVararg
        addAnnotationsFrom(session, this@toFirValueParameter, javaTypeParameterStack)
    }
}

internal fun JavaType?.toConeProjection(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    boundTypeParameter: FirTypeParameter?,
    nullability: ConeNullability = ConeNullability.NOT_NULL
): ConeKotlinTypeProjection {
    return when (this) {
        null -> ConeStarProjection
        is JavaWildcardType -> {
            val bound = this.bound
            val argumentVariance = if (isExtends) OUT_VARIANCE else IN_VARIANCE
            val parameterVariance = boundTypeParameter?.variance ?: INVARIANT
            if (bound == null || parameterVariance != INVARIANT && parameterVariance != argumentVariance) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, nullability)
                if (argumentVariance == OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        is JavaClassifierType -> toConeKotlinTypeWithNullability(session, javaTypeParameterStack, nullability)
        else -> ConeClassErrorType("Unexpected type argument: $this")
    }
}

private fun JavaAnnotationArgument.toFirExpression(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirExpression {
    // TODO: this.name
    return when (this) {
        is JavaLiteralAnnotationArgument -> {
            value.createConstant(session)
        }
        is JavaArrayAnnotationArgument -> buildArrayOfCall {
            for (element in getElements()) {
                arguments += element.toFirExpression(session, javaTypeParameterStack)
            }
        }
        is JavaEnumValueAnnotationArgument -> {
            buildFunctionCall {
                val classId = this@toFirExpression.enumClassId
                val entryName = this@toFirExpression.entryName
                val calleeReference = if (classId != null && entryName != null) {
                    val callableSymbol = session.firSymbolProvider.getClassDeclaredCallableSymbols(
                        classId, entryName
                    ).firstOrNull()
                    callableSymbol?.let {
                        buildResolvedNamedReference {
                            name = entryName
                            resolvedSymbol = it
                        }
                    }
                } else {
                    null
                }
                this.calleeReference = calleeReference
                    ?: buildErrorNamedReference {
                        diagnostic = FirSimpleDiagnostic("Strange Java enum value: $classId.$entryName", DiagnosticKind.Java)
                    }
            }
        }
        is JavaClassObjectAnnotationArgument -> buildGetClassCall {
            val referencedType = getReferencedType()
            arguments += buildClassReferenceExpression {
                classTypeRef = referencedType.toFirResolvedTypeRef(session, javaTypeParameterStack)
            }
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session, javaTypeParameterStack)
        else -> buildErrorExpression {
            diagnostic = FirSimpleDiagnostic("Unknown JavaAnnotationArgument: ${this::class.java}", DiagnosticKind.Java)
        }
    }
}

// TODO: use kind here
private fun <T> List<T>.createArrayOfCall(session: FirSession, @Suppress("UNUSED_PARAMETER") kind: FirConstKind<T>): FirArrayOfCall {
    return buildArrayOfCall {
        for (element in this@createArrayOfCall) {
            arguments += element.createConstant(session)
        }
    }
}

internal fun Any?.createConstant(session: FirSession): FirExpression {
    return when (this) {
        is Byte -> buildConstExpression(null, FirConstKind.Byte, this)
        is Short -> buildConstExpression(null, FirConstKind.Short, this)
        is Int -> buildConstExpression(null, FirConstKind.Int, this)
        is Long -> buildConstExpression(null, FirConstKind.Long, this)
        is Char -> buildConstExpression(null, FirConstKind.Char, this)
        is Float -> buildConstExpression(null, FirConstKind.Float, this)
        is Double -> buildConstExpression(null, FirConstKind.Double, this)
        is Boolean -> buildConstExpression(null, FirConstKind.Boolean, this)
        is String -> buildConstExpression(null, FirConstKind.String, this)
        is ByteArray -> toList().createArrayOfCall(session, FirConstKind.Byte)
        is ShortArray -> toList().createArrayOfCall(session, FirConstKind.Short)
        is IntArray -> toList().createArrayOfCall(session, FirConstKind.Int)
        is LongArray -> toList().createArrayOfCall(session, FirConstKind.Long)
        is CharArray -> toList().createArrayOfCall(session, FirConstKind.Char)
        is FloatArray -> toList().createArrayOfCall(session, FirConstKind.Float)
        is DoubleArray -> toList().createArrayOfCall(session, FirConstKind.Double)
        is BooleanArray -> toList().createArrayOfCall(session, FirConstKind.Boolean)
        null -> buildConstExpression(null, FirConstKind.Null, null)

        else -> buildErrorExpression {
            diagnostic = FirSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
        }
    }
}

private fun JavaType.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirResolvedTypeRef {
    if (this is JavaClassifierType) return toFirResolvedTypeRef(session, javaTypeParameterStack)
    return buildResolvedTypeRef {
        type = ConeClassErrorType("Unexpected JavaType: $this")
    }
}

