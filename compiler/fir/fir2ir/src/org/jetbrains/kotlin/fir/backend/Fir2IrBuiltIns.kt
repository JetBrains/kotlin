/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class Fir2IrBuiltIns(
    private val components: Fir2IrComponents,
    private val provider: Fir2IrSpecialSymbolProvider
) : Fir2IrComponents by components {
    init {
        provider.initComponents(components)
    }

    // ---------------------- special annotations ----------------------

    private val enhancedNullabilityAnnotationIrSymbol by lazy {
        specialAnnotationIrSymbolById(StandardClassIds.Annotations.EnhancedNullability)
    }

    internal fun enhancedNullabilityAnnotationConstructorCall(): IrConstructorCall? =
        enhancedNullabilityAnnotationIrSymbol?.toConstructorCall()

    private val flexibleNullabilityAnnotationSymbol by lazy {
        specialAnnotationIrSymbolById(StandardClassIds.Annotations.FlexibleNullability)
    }

    internal fun flexibleNullabilityAnnotationConstructorCall(): IrConstructorCall? =
        flexibleNullabilityAnnotationSymbol?.toConstructorCall()

    private val flexibleMutabilityAnnotationSymbol by lazy {
        specialAnnotationIrSymbolById(StandardClassIds.Annotations.FlexibleMutability)
    }

    internal fun flexibleMutabilityAnnotationConstructorCall(): IrConstructorCall? =
        flexibleMutabilityAnnotationSymbol?.toConstructorCall()

    private val flexibleArrayElementVarianceAnnotationSymbol by lazy {
        specialAnnotationIrSymbolById(StandardClassIds.Annotations.FlexibleArrayElementVariance)
    }

    internal fun flexibleArrayElementVarianceAnnotationConstructorCall(): IrConstructorCall? =
        flexibleArrayElementVarianceAnnotationSymbol?.toConstructorCall()

    private val rawTypeAnnotationSymbol by lazy {
        specialAnnotationIrSymbolById(StandardClassIds.Annotations.RawTypeAnnotation)
    }

    internal fun rawTypeAnnotationConstructorCall(): IrConstructorCall? =
        rawTypeAnnotationSymbol?.toConstructorCall()

    // ---------------------- regular annotations ----------------------

    private val extensionFunctionTypeAnnotationFirSymbol by lazy {
        regularAnnotationFirSymbolById(StandardClassIds.Annotations.ExtensionFunctionType)
    }

    private val extensionFunctionTypeAnnotationSymbol by lazy {
        extensionFunctionTypeAnnotationFirSymbol?.toSymbol(ConversionTypeOrigin.DEFAULT) as? IrClassSymbol
    }

    internal fun extensionFunctionTypeAnnotationConstructorCall(): IrConstructorCall? =
        extensionFunctionTypeAnnotationSymbol?.toConstructorCall(extensionFunctionTypeAnnotationFirSymbol!!)

    // ---------------------- utils ----------------------

    private fun specialAnnotationIrSymbolById(classId: ClassId): IrClassSymbol? {
        return provider.getClassSymbolById(classId)
    }

    private fun regularAnnotationFirSymbolById(id: ClassId): FirRegularClassSymbol? {
        return session.symbolProvider.getClassLikeSymbolByClassId(id) as? FirRegularClassSymbol
    }

    private fun IrClassSymbol.toConstructorCall(firSymbol: FirRegularClassSymbol? = null): IrConstructorCallImpl? {
        val constructorSymbol = if (firSymbol == null) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            owner.declarations.firstIsInstance<IrConstructor>().symbol
        } else {
            val firConstructorSymbol = firSymbol.unsubstitutedScope().getDeclaredConstructors().singleOrNull() ?: return null

            declarationStorage.getIrConstructorSymbol(firConstructorSymbol)
        }
        return IrConstructorCallImpl.fromSymbolOwner(defaultType, constructorSymbol)
    }
}
