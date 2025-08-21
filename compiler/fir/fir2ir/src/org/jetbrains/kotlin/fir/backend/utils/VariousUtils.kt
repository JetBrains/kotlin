/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.isBoxedArray
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails
import kotlin.collections.set

context(c: Fir2IrComponents)
fun FirRegularClass.getIrSymbolsForSealedSubclasses(): List<IrClassSymbol> {
    val symbolProvider = c.session.symbolProvider
    return getSealedClassInheritors(c.session).mapNotNull {
        symbolProvider.getClassLikeSymbolByClassId(it)?.toIrSymbol()
    }.filterIsInstance<IrClassSymbol>()
}

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
context(c: Fir2IrComponents)
internal fun FirVariable.irTypeForPotentiallyComponentCall(predefinedType: IrType? = null): IrType {
    val initializer = initializer
    val typeRef = when {
        isVal && initializer is FirComponentCall -> initializer.resolvedType
        else -> {
            if (predefinedType != null) return predefinedType
            this.returnTypeRef.coneType
        }
    }
    return typeRef.toIrType()
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

context(c: Fir2IrComponents)
internal fun FirQualifiedAccessExpression.buildSubstitutorByCalledCallable(): ConeSubstitutor {
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
    return substitutorByMap(map, c.session)
}

internal inline fun <R> convertCatching(element: FirElement, conversionScope: Fir2IrConversionScope? = null, block: () -> R): R {
    try {
        return block()
    } catch (e: Throwable) {
        rethrowExceptionWithDetails("Exception was thrown during transformation of ${element::class.java}", e) {
            withFirEntry("element", element)
            conversionScope?.containingFileIfAny()?.let { withEntry("file", it.path) }
        }
    }
}

fun IrType.getArrayElementType(builtins: Fir2IrBuiltinSymbolsContainer): IrType {
    return when {
        isBoxedArray -> {
            when (val argument = (this as IrSimpleType).arguments.singleOrNull()) {
                is IrTypeProjection ->
                    argument.type
                is IrStarProjection ->
                    builtins.anyNType
                null ->
                    error("Unexpected array argument type: null")
            }
        }
        else -> {
            val classifier = this.classOrNull!!
            builtins.primitiveArrayElementTypes[classifier]
                ?: builtins.unsignedArraysElementTypes[classifier]
                ?: error("Primitive array expected: $classifier")
        }
    }
}

val IrClassSymbol.defaultTypeWithoutArguments: IrSimpleType
    get() = IrSimpleTypeImpl(
        classifier = this,
        nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
        arguments = emptyList(),
        annotations = emptyList()
    )

val FirCallableSymbol<*>.isInlineClassProperty: Boolean
    get() {
        if (this !is FirPropertySymbol || dispatchReceiverType == null || receiverParameterSymbol != null || hasContextParameters) return false
        val containingClass = getContainingClassSymbol() as? FirRegularClassSymbol ?: return false
        val inlineClassRepresentation = containingClass.fir.inlineClassRepresentation ?: return false
        return inlineClassRepresentation.underlyingPropertyName == this.name
    }

fun FirBasedSymbol<*>.shouldHaveReceiver(session: FirSession): Boolean =
    !fir.hasAnnotation(StandardClassIds.Annotations.jsNoDispatchReceiver, session)

fun FirQualifiedAccessExpression.isConstructorCallOnTypealiasWithInnerRhs(): Boolean {
    return (calleeReference.symbol as? FirConstructorSymbol)?.let {
        it.origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor && it.receiverParameterSymbol != null
    } == true
}

internal fun <T : FirDeclaration, R> filterOutSymbolsFromCache(cache: Map<T, R>, filterOutSymbols: Set<FirBasedSymbol<*>>): Map<T, R> {
    return cache.filterKeys { !filterOutSymbols.contains(it.symbol) }
}