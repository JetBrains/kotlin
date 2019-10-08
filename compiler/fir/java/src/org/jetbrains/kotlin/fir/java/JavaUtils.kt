/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.resolve.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
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
    return ConeClassTypeImpl(lookupTag, typeArguments, isNullable)
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
        else -> ConeKotlinErrorType("Unexpected type reference in JavaClassUseSiteScope: ${this::class.java}")
    }

internal fun JavaType?.toNotNullConeKotlinType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): ConeLookupTagBasedType {
    return toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = false)
}

internal fun JavaType.toFirJavaTypeRef(session: FirSession, javaTypeParameterStack: JavaTypeParameterStack): FirJavaTypeRef {
    val annotations = (this as? JavaClassifierType)?.annotations.orEmpty()
    return FirJavaTypeRef(
        annotations = annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) },
        type = this
    )
}

internal fun JavaClassifierType.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirResolvedTypeRef {
    val coneType = this.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = false)
    return FirResolvedTypeRefImpl(
        psi = null, type = coneType,
        annotations = annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) }
    )
}

internal fun JavaType?.toConeKotlinTypeWithNullability(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack, isNullable: Boolean
): ConeLookupTagBasedType {
    return when (this) {
        is JavaClassifierType -> {
            toConeKotlinTypeWithNullability(session, isNullable, javaTypeParameterStack)
        }
        is JavaPrimitiveType -> {
            val primitiveType = type
            val kotlinPrimitiveName = when (val javaName = primitiveType?.typeName?.asString()) {
                null -> "Unit"
                else -> javaName.capitalize()
            }
            val classId = StandardClassIds.byName(kotlinPrimitiveName)
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        is JavaArrayType -> {
            val componentType = componentType
            if (componentType !is JavaPrimitiveType) {
                val classId = StandardClassIds.Array
                val argumentType = ConeFlexibleType(
                    componentType.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = false),
                    componentType.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = true)
                )
                classId.toConeKotlinType(arrayOf(argumentType), isNullable)
            } else {
                val javaComponentName = componentType.type?.typeName?.asString()?.capitalize() ?: error("Array of voids")
                val classId = StandardClassIds.byName(javaComponentName + "Array")
                classId.toConeKotlinType(emptyArray(), isNullable)
            }
        }
        is JavaWildcardType -> bound?.toNotNullConeKotlinType(session, javaTypeParameterStack) ?: run {
            val classId = StandardClassIds.Any
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        null -> {
            val classId = StandardClassIds.Any
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

internal fun JavaClassifierType.toConeKotlinTypeWithNullability(
    session: FirSession, isNullable: Boolean, javaTypeParameterStack: JavaTypeParameterStack
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            //val classId = classifier.classId!!
            var classId = JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!) ?: classifier.classId!!
            classId = classId.readOnlyToMutable() ?: classId

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            lookupTag.constructClassType(
                typeArguments.mapIndexed { index, argument ->
                    argument.toConeProjection(
                        session, javaTypeParameterStack, null
                        //symbol.fir.typeParameters.getOrNull(index)
                    )
                }.toTypedArray(), isNullable
            )
        }
        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            ConeTypeParameterTypeImpl(symbol.toLookupTag(), isNullable)
        }
        else -> ConeClassErrorType(reason = "Unexpected classifier: $classifier")
    }
}

internal fun JavaAnnotation.toFirAnnotationCall(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirAnnotationCall {
    return FirAnnotationCallImpl(
        psi = null, useSiteTarget = null,
        annotationTypeRef = FirResolvedTypeRefImpl(
            psi = null,
            type = ConeClassTypeImpl(FirClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false),
            annotations = emptyList()
        )
    ).apply {
        for (argument in this@toFirAnnotationCall.arguments) {
            arguments += argument.toFirExpression(session, javaTypeParameterStack)
        }
    }
}

internal fun FirAbstractAnnotatedElement.addAnnotationsFrom(
    session: FirSession, javaAnnotationOwner: JavaAnnotationOwner, javaTypeParameterStack: JavaTypeParameterStack
) {
    for (annotation in javaAnnotationOwner.annotations) {
        annotations += annotation.toFirAnnotationCall(session, javaTypeParameterStack)
    }
}

internal fun JavaValueParameter.toFirValueParameters(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return FirJavaValueParameter(
        session, (this as? JavaElementImpl<*>)?.psi, name ?: Name.special("<anonymous Java parameter>"),
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack),
        isVararg = isVararg
    ).apply {
        addAnnotationsFrom(session, this@toFirValueParameters, javaTypeParameterStack)
    }
}

internal fun JavaType?.toConeProjection(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack, boundTypeParameter: FirTypeParameter?
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
                val boundType = bound.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = false)
                if (argumentVariance == OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        is JavaClassifierType -> toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable = false)
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
        is JavaArrayAnnotationArgument -> FirArrayOfCallImpl(null).apply {
            for (element in getElements()) {
                arguments += element.toFirExpression(session, javaTypeParameterStack)
            }
        }
        is JavaEnumValueAnnotationArgument -> {
            FirFunctionCallImpl(null).apply {
                val classId = this@toFirExpression.enumClassId
                val entryName = this@toFirExpression.entryName
                val calleeReference = if (classId != null && entryName != null) {
                    val callableSymbol = session.service<FirSymbolProvider>().getClassDeclaredCallableSymbols(
                        classId, entryName
                    ).firstOrNull()
                    callableSymbol?.let {
                        FirResolvedCallableReferenceImpl(null, entryName, it)
                    }
                } else {
                    null
                }
                this.calleeReference = calleeReference
                    ?: FirErrorNamedReference(null, "Strange Java enum value: $classId.$entryName")
            }
        }
        is JavaClassObjectAnnotationArgument -> FirGetClassCallImpl(null).apply {
            val referencedType = getReferencedType()
            arguments += FirClassReferenceExpressionImpl(
                null, referencedType.toFirResolvedTypeRef(session, javaTypeParameterStack)
            )
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session, javaTypeParameterStack)
        else -> FirErrorExpressionImpl(null, "Unknown JavaAnnotationArgument: ${this::class.java}")
    }
}

// TODO: use kind here
private fun <T> List<T>.createArrayOfCall(session: FirSession, @Suppress("UNUSED_PARAMETER") kind: IrConstKind<T>): FirArrayOfCall {
    return FirArrayOfCallImpl(null).apply {
        for (element in this@createArrayOfCall) {
            arguments += element.createConstant(session)
        }
    }
}

internal fun Any?.createConstant(session: FirSession): FirExpression {
    return when (this) {
        is Byte -> FirConstExpressionImpl(null, IrConstKind.Byte, this)
        is Short -> FirConstExpressionImpl(null, IrConstKind.Short, this)
        is Int -> FirConstExpressionImpl(null, IrConstKind.Int, this)
        is Long -> FirConstExpressionImpl(null, IrConstKind.Long, this)
        is Char -> FirConstExpressionImpl(null, IrConstKind.Char, this)
        is Float -> FirConstExpressionImpl(null, IrConstKind.Float, this)
        is Double -> FirConstExpressionImpl(null, IrConstKind.Double, this)
        is Boolean -> FirConstExpressionImpl(null, IrConstKind.Boolean, this)
        is String -> FirConstExpressionImpl(null, IrConstKind.String, this)
        is ByteArray -> toList().createArrayOfCall(session, IrConstKind.Byte)
        is ShortArray -> toList().createArrayOfCall(session, IrConstKind.Short)
        is IntArray -> toList().createArrayOfCall(session, IrConstKind.Int)
        is LongArray -> toList().createArrayOfCall(session, IrConstKind.Long)
        is CharArray -> toList().createArrayOfCall(session, IrConstKind.Char)
        is FloatArray -> toList().createArrayOfCall(session, IrConstKind.Float)
        is DoubleArray -> toList().createArrayOfCall(session, IrConstKind.Double)
        is BooleanArray -> toList().createArrayOfCall(session, IrConstKind.Boolean)
        null -> FirConstExpressionImpl(null, IrConstKind.Null, null)

        else -> FirErrorExpressionImpl(null, "Unknown value in JavaLiteralAnnotationArgument: $this")
    }
}

private fun JavaType.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirResolvedTypeRef {
    if (this is JavaClassifierType) return toFirResolvedTypeRef(session, javaTypeParameterStack)
    return FirResolvedTypeRefImpl(
        psi = null, type = ConeClassErrorType("Unexpected JavaType: $this"),
        annotations = emptyList()
    )
}


