/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirSourceElement
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
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
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack, isNullable: Boolean = false
): FirResolvedTypeRef {
    val coneType = this.toConeKotlinTypeWithNullability(session, javaTypeParameterStack, isNullable)
    return FirResolvedTypeRefImpl(
        source = null, type = coneType
    ).apply {
        annotations += this@toFirResolvedTypeRef.annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) }
    }
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
        source = null, useSiteTarget = null,
        annotationTypeRef = FirResolvedTypeRefImpl(
            source = null,
            type = ConeClassLikeTypeImpl(FirRegularClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false)
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

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession, index: Int, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return FirJavaValueParameter(
        session, (this as? JavaElementImpl<*>)?.psi?.toFirSourceElement(), name ?: Name.identifier("p$index"),
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack),
        isVararg = isVararg
    ).apply {
        addAnnotationsFrom(session, this@toFirValueParameter, javaTypeParameterStack)
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
                    val callableSymbol = session.firSymbolProvider.getClassDeclaredCallableSymbols(
                        classId, entryName
                    ).firstOrNull()
                    callableSymbol?.let {
                        FirResolvedNamedReferenceImpl(null, entryName, it)
                    }
                } else {
                    null
                }
                this.calleeReference = calleeReference
                    ?: FirErrorNamedReferenceImpl(null, FirSimpleDiagnostic("Strange Java enum value: $classId.$entryName", DiagnosticKind.Java))
            }
        }
        is JavaClassObjectAnnotationArgument -> FirGetClassCallImpl(null).apply {
            val referencedType = getReferencedType()
            arguments += FirClassReferenceExpressionImpl(
                null, referencedType.toFirResolvedTypeRef(session, javaTypeParameterStack)
            )
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session, javaTypeParameterStack)
        else -> FirErrorExpressionImpl(null, FirSimpleDiagnostic("Unknown JavaAnnotationArgument: ${this::class.java}", DiagnosticKind.Java))
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

        else -> FirErrorExpressionImpl(null, FirSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java))
    }
}

private fun JavaType.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirResolvedTypeRef {
    if (this is JavaClassifierType) return toFirResolvedTypeRef(session, javaTypeParameterStack)
    return FirResolvedTypeRefImpl(
        source = null, type = ConeClassErrorType("Unexpected JavaType: $this")
    )
}

internal fun FirFunction<*>.computeJvmDescriptor(): String = buildString {
    if (this@computeJvmDescriptor is FirJavaMethod) {
        append(name.asString())
    } else {
        append("<init>")
    }

    append("(")
    for (parameter in valueParameters) {
        appendErasedType(parameter.returnTypeRef)
    }
    append(")")

    if (this@computeJvmDescriptor !is FirJavaMethod || (returnTypeRef as FirJavaTypeRef).isVoid()) {
        append("V")
    } else {
        appendErasedType(returnTypeRef)
    }
}

// TODO: primitive types, arrays, etc.
private fun StringBuilder.appendErasedType(typeRef: FirTypeRef) {
    fun appendClass(klass: JavaClass) {
        klass.fqName?.let {
            append("L")
            append(it.asString().replace(".", "/"))
        }
    }

    when (typeRef) {
        is FirResolvedTypeRef -> appendConeType(typeRef.type)
        is FirJavaTypeRef -> {
            when (val javaType = typeRef.type) {
                is JavaClassifierType -> {
                    when (val classifier = javaType.classifier) {
                        is JavaClass -> appendClass(classifier)
                        is JavaTypeParameter -> {
                            val representative = classifier.upperBounds.firstOrNull { it.classifier is JavaClass }
                            if (representative == null) {
                                append("Ljava/lang/Object")
                            } else {
                                appendClass(representative.classifier as JavaClass)
                            }
                        }
                        else -> return
                    }
                    append(";")
                }
            }
        }
    }
}

private fun StringBuilder.appendConeType(coneType: ConeKotlinType) {
    fun appendClassLikeType(type: ConeClassLikeType) {
        append("L")
        val classId = type.lookupTag.classId
        append(classId.packageFqName.asString().replace(".", "/"))
        append("/")
        append(classId.relativeClassName)
    }

    if (coneType is ConeClassErrorType) return
    when (coneType) {
        is ConeClassLikeType -> {
            appendClassLikeType(coneType)
        }
        is ConeTypeParameterType -> {
            val representative = coneType.lookupTag.typeParameterSymbol.fir.bounds.firstOrNull {
                (it as? FirResolvedTypeRef)?.type is ConeClassLikeType
            }
            if (representative == null) {
                append("Ljava/lang/Object")
            } else {
                appendClassLikeType(representative.coneTypeUnsafe())
            }
            append(coneType.lookupTag.name)
        }
    }
    append(";")
}

private fun FirJavaTypeRef.isVoid(): Boolean {
    return type is JavaPrimitiveType && type.type == null
}



