/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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

internal fun FirTypeRef.toNotNullConeKotlinType(session: FirSession): ConeKotlinType =
    when (this) {
        is FirResolvedTypeRef -> type
        is FirJavaTypeRef -> {
            val javaType = type
            javaType.toNotNullConeKotlinType(session)
        }
        else -> ConeKotlinErrorType("Unexpected type reference in JavaClassUseSiteScope: ${this::class.java}")
    }

internal fun JavaType?.toNotNullConeKotlinType(session: FirSession): ConeLookupTagBasedType {
    return toConeKotlinTypeWithNullability(session, isNullable = false)
}

internal fun JavaType.toFirJavaTypeRef(session: FirSession): FirJavaTypeRef {
    val annotations = (this as? JavaClassifierType)?.annotations.orEmpty()
    return FirJavaTypeRef(session, annotations = annotations.map { it.toFirAnnotationCall(session) }, type = this)
}

internal fun JavaClassifierType.toFirResolvedTypeRef(session: FirSession): FirResolvedTypeRef {
    val coneType = this.toConeKotlinTypeWithNullability(session, isNullable = false)
    return FirResolvedTypeRefImpl(
        session, psi = null, type = coneType,
        isMarkedNullable = false, annotations = annotations.map { it.toFirAnnotationCall(session) }
    )
}

internal fun JavaType?.toConeKotlinTypeWithNullability(session: FirSession, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is JavaClassifierType -> {
            toConeKotlinTypeWithNullability(session, isNullable)
        }
        is JavaPrimitiveType -> {
            val primitiveType = type
            val kotlinPrimitiveName = when (val javaName = primitiveType?.typeName?.asString()) {
                null -> "Unit"
                else -> javaName.capitalize()
            }
            val classId = ClassId(FqName("kotlin"), FqName(kotlinPrimitiveName), false)
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        is JavaArrayType -> {
            val componentType = componentType
            if (componentType !is JavaPrimitiveType) {
                val classId = ClassId(FqName("kotlin"), FqName("Array"), false)
                val argumentType = ConeFlexibleType(
                    componentType.toConeKotlinTypeWithNullability(session, isNullable = false),
                    componentType.toConeKotlinTypeWithNullability(session, isNullable = true)
                )
                classId.toConeKotlinType(arrayOf(argumentType), isNullable)
            } else {
                val javaComponentName = componentType.type?.typeName?.asString()?.capitalize() ?: error("Array of voids")
                val classId = ClassId(FqName("kotlin"), FqName(javaComponentName + "Array"), false)
                classId.toConeKotlinType(emptyArray(), isNullable)
            }
        }
        is JavaWildcardType -> bound?.toNotNullConeKotlinType(session) ?: run {
            val classId = ClassId(FqName("kotlin"), FqName("Any"), false)
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        null -> {
            val classId = ClassId(FqName("kotlin"), FqName("Any"), false)
            classId.toConeKotlinType(emptyArray(), isNullable)
        }
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

internal fun JavaClassifierType.toConeKotlinTypeWithNullability(session: FirSession, isNullable: Boolean): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            val classId = classifier.classId!!
            val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(classId) as? FirClassSymbol
            symbol?.constructType(
                typeArguments.mapIndexed { index, argument ->
                    argument.toConeProjection(session, symbol.fir.typeParameters.getOrNull(index))
                }.toTypedArray(), isNullable
            ) ?: ConeClassErrorType("Symbol not found, for `$classId`")
        }
        is JavaTypeParameter -> {
            // TODO: it's unclear how to identify type parameter by the symbol
            // TODO: some type parameter cache (provider?)
            val symbol = createTypeParameterSymbol(session, classifier.name)
            ConeTypeParameterTypeImpl(symbol, isNullable)
        }
        else -> ConeClassErrorType(reason = "Unexpected classifier: $classifier")
    }
}

internal fun createTypeParameterSymbol(session: FirSession, name: Name): FirTypeParameterSymbol {
    val firSymbol = FirTypeParameterSymbol()
    FirTypeParameterImpl(session, null, firSymbol, name, variance = INVARIANT, isReified = false)
    return firSymbol
}

internal fun JavaAnnotation.toFirAnnotationCall(session: FirSession): FirAnnotationCall {
    return FirAnnotationCallImpl(
        session, psi = null, useSiteTarget = null,
        annotationTypeRef = FirResolvedTypeRefImpl(
            session,
            psi = null,
            type = ConeClassTypeImpl(FirClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false),
            isMarkedNullable = true,
            annotations = emptyList()
        )
    ).apply {
        for (argument in this@toFirAnnotationCall.arguments) {
            arguments += argument.toFirExpression(session)
        }
    }
}

internal fun FirAbstractAnnotatedElement.addAnnotationsFrom(session: FirSession, javaAnnotationOwner: JavaAnnotationOwner) {
    for (annotation in javaAnnotationOwner.annotations) {
        annotations += annotation.toFirAnnotationCall(session)
    }
}

internal fun JavaValueParameter.toFirValueParameters(session: FirSession): FirValueParameter {
    return FirJavaValueParameter(
        session, name ?: Name.special("<anonymous Java parameter>"),
        returnTypeRef = type.toFirJavaTypeRef(session),
        isVararg = isVararg
    ).apply {
        addAnnotationsFrom(session, this@toFirValueParameters)
    }
}

internal fun JavaType?.toConeProjection(session: FirSession, boundTypeParameter: FirTypeParameter?): ConeKotlinTypeProjection {
    return when (this) {
        null -> ConeStarProjection
        is JavaWildcardType -> {
            val bound = this.bound
            val argumentVariance = if (isExtends) OUT_VARIANCE else IN_VARIANCE
            val parameterVariance = boundTypeParameter?.variance ?: INVARIANT
            if (bound == null || parameterVariance != INVARIANT && parameterVariance != argumentVariance) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinTypeWithNullability(session, isNullable = false)
                if (argumentVariance == OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        is JavaClassifierType -> toConeKotlinTypeWithNullability(session, isNullable = false)
        else -> ConeClassErrorType("Unexpected type argument: $this")
    }
}

private fun JavaAnnotationArgument.toFirExpression(session: FirSession): FirExpression {
    // TODO: this.name
    return when (this) {
        is JavaLiteralAnnotationArgument -> {
            when (val value = value) {
                is ByteArray -> value.toList().createArrayOfCall(session, IrConstKind.Byte)
                is ShortArray -> value.toList().createArrayOfCall(session, IrConstKind.Short)
                is IntArray -> value.toList().createArrayOfCall(session, IrConstKind.Int)
                is LongArray -> value.toList().createArrayOfCall(session, IrConstKind.Long)
                is CharArray -> value.toList().createArrayOfCall(session, IrConstKind.Char)
                is FloatArray -> value.toList().createArrayOfCall(session, IrConstKind.Float)
                is DoubleArray -> value.toList().createArrayOfCall(session, IrConstKind.Double)
                is BooleanArray -> value.toList().createArrayOfCall(session, IrConstKind.Boolean)
                else -> value.createConstant(session)
            }
        }
        is JavaArrayAnnotationArgument -> FirArrayOfCallImpl(session, null).apply {
            for (element in getElements()) {
                arguments += element.toFirExpression(session)
            }
        }
        is JavaEnumValueAnnotationArgument -> {
            FirFunctionCallImpl(session, null).apply {
                val classId = this@toFirExpression.enumClassId
                val entryName = this@toFirExpression.entryName
                val calleeReference = if (classId != null && entryName != null) {
                    val callableSymbol = session.service<FirSymbolProvider>().getCallableSymbols(
                        CallableId(classId.packageFqName, classId.relativeClassName, entryName)
                    ).firstOrNull()
                    callableSymbol?.let {
                        FirResolvedCallableReferenceImpl(session, null, entryName, it)
                    }
                } else {
                    null
                }
                this.calleeReference = calleeReference
                    ?: FirErrorNamedReference(session, null, "Strange Java enum value: $classId.$entryName")
            }
        }
        is JavaClassObjectAnnotationArgument -> FirGetClassCallImpl(session, null).apply {
            val referencedType = getReferencedType()
            arguments += FirClassReferenceExpressionImpl(session, null, referencedType.toFirResolvedTypeRef(session))
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session)
        else -> FirErrorExpressionImpl(session, null, "Unknown JavaAnnotationArgument: ${this::class.java}")
    }
}

// TODO: use kind here
private fun <T> List<T>.createArrayOfCall(session: FirSession, @Suppress("UNUSED_PARAMETER") kind: IrConstKind<T>): FirArrayOfCall {
    return FirArrayOfCallImpl(session, null).apply {
        for (element in this@createArrayOfCall) {
            arguments += element.createConstant(session)
        }
    }
}

private fun Any?.createConstant(session: FirSession): FirExpression {
    return when (this) {
        is Byte -> FirConstExpressionImpl(session, null, IrConstKind.Byte, this)
        is Short -> FirConstExpressionImpl(session, null, IrConstKind.Short, this)
        is Int -> FirConstExpressionImpl(session, null, IrConstKind.Int, this)
        is Long -> FirConstExpressionImpl(session, null, IrConstKind.Long, this)
        is Char -> FirConstExpressionImpl(session, null, IrConstKind.Char, this)
        is Float -> FirConstExpressionImpl(session, null, IrConstKind.Float, this)
        is Double -> FirConstExpressionImpl(session, null, IrConstKind.Double, this)
        is Boolean -> FirConstExpressionImpl(session, null, IrConstKind.Boolean, this)
        is String -> FirConstExpressionImpl(session, null, IrConstKind.String, this)
        null -> FirConstExpressionImpl(session, null, IrConstKind.Null, null)

        else -> FirErrorExpressionImpl(session, null, "Unknown value in JavaLiteralAnnotationArgument: $this")
    }
}

private fun JavaType.toFirResolvedTypeRef(session: FirSession): FirResolvedTypeRef {
    if (this is JavaClassifierType) return toFirResolvedTypeRef(session)
    return FirResolvedTypeRefImpl(
        session, psi = null, type = ConeClassErrorType("Unexpected JavaType: $this"),
        isMarkedNullable = false, annotations = emptyList()
    )
}


