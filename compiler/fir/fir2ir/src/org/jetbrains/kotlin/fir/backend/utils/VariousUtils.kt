/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.filterIsInstance
import kotlin.collections.getOrNull
import kotlin.collections.mapNotNull
import kotlin.collections.mapNotNullTo
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.set
import kotlin.collections.withIndex

fun FirRegularClass.getIrSymbolsForSealedSubclasses(c: Fir2IrComponents): List<IrClassSymbol> {
    val symbolProvider = c.session.symbolProvider
    return getSealedClassInheritors(c.session).mapNotNull {
        symbolProvider.getClassLikeSymbolByClassId(it)?.toSymbol(c)
    }.filterIsInstance<IrClassSymbol>()
}

fun FirCallableDeclaration.contextReceiversForFunctionOrContainingProperty(): List<FirContextReceiver> =
    if (this is FirPropertyAccessor)
        this.propertySymbol.fir.contextReceivers
    else
        this.contextReceivers

fun List<IrDeclaration>.extractFirDeclarations(): Set<FirDeclaration> {
    return this.mapTo(mutableSetOf()) { ((it as IrMetadataSourceOwner).metadata as FirMetadataSource).fir }
}

/**
 * Note: for componentN call, we have to change the type here (to the original component type) to keep compatibility with PSI2IR
 * Some backend optimizations related to withIndex() probably depend on this type: index should always be Int
 * See e.g. forInStringWithIndexWithExplicitlyTypedIndexVariable.kt from codegen box tests
 *
 * [predefinedType] is needed for case, when this function is used to convert some variable access, and
 *   default IR type, for it is already known
 *   It's not correct to always use converted [this.returnTypeRef] in one particular case:
 *
 * val <T> T.some: T
 *     get() = ...
 *     set(value) {
 *         field = value <----
 *     }
 *
 *  Here `value` has type `T`. In FIR there is one type parameter `T` for the whole property
 *  But in IR we have different type parameters for getter and setter. And by default `toIrType(c)` transforms
 *    `T` as type parameter of getter, but here we are in context of the setter. And in CallAndReferenceGenerator.convertToIrCall
 *    we already know that `value` should have type `T[set-some]`, so this type is provided as [predefinedType]
 *
 *  The alternative could be to determine outside that we are in scope of setter and pass type origin, but it's
 *    much more complicated and messy
 */
internal fun FirVariable.irTypeForPotentiallyComponentCall(c: Fir2IrComponents, predefinedType: IrType? = null): IrType {
    val initializer = initializer
    val typeRef = when {
        isVal && initializer is FirComponentCall -> initializer.resolvedType
        else -> {
            if (predefinedType != null) return predefinedType
            this.returnTypeRef.coneType
        }
    }
    return typeRef.toIrType(c)
}

internal val FirValueParameter.varargElementType: ConeKotlinType?
    get() {
        if (!isVararg) return null
        return returnTypeRef.coneType.arrayElementType()
    }


internal fun implicitCast(original: IrExpression, castType: IrType, typeOperator: IrTypeOperator): IrExpression {
    if (original.type == castType) {
        return original
    }
    if (original is IrTypeOperatorCall) {
        return implicitCast(original.argument, castType, typeOperator)
    }
    return IrTypeOperatorCallImpl(
        original.startOffset,
        original.endOffset,
        castType,
        typeOperator,
        castType,
        original
    )
}

internal fun FirQualifiedAccessExpression.buildSubstitutorByCalledCallable(c: Fir2IrComponents): ConeSubstitutor {
    val typeParameters = when (val declaration = calleeReference.toResolvedCallableSymbol()?.fir) {
        is FirFunction -> declaration.typeParameters
        is FirProperty -> declaration.typeParameters
        else -> return ConeSubstitutor.Empty
    }
    val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    for ((index, typeParameter) in typeParameters.withIndex()) {
        val typeProjection = typeArguments.getOrNull(index) as? FirTypeProjectionWithVariance ?: continue
        map[typeParameter.symbol] = typeProjection.typeRef.coneType
    }
    return ConeSubstitutorByMap.create(map, c.session)
}

internal inline fun <R> convertCatching(element: FirElement, conversionScope: Fir2IrConversionScope? = null, block: () -> R): R {
    try {
        return block()
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        errorWithAttachment("Exception was thrown during transformation of ${element::class.java}", cause = e) {
            withFirEntry("element", element)
            conversionScope?.containingFileIfAny()?.let { withEntry("file", it.path) }
        }
    }
}
