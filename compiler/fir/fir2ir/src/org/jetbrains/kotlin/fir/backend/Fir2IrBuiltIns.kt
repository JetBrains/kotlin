/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class Fir2IrBuiltIns(
    private val components: Fir2IrComponents,
    private val provider: Fir2IrSpecialSymbolProvider?
) : Fir2IrComponents by components {
    init {
        provider?.initComponents(components)
    }

    private val enhancedNullabilityAnnotationSymbol by lazy {
        annotationSymbolById(StandardClassIds.Annotations.EnhancedNullability)
    }

    internal fun enhancedNullabilityAnnotationConstructorCall(): IrConstructorCall? =
        enhancedNullabilityAnnotationSymbol?.toConstructorCall()

    private val flexibleNullabilityAnnotationSymbol by lazy {
        annotationSymbolById(StandardClassIds.Annotations.FlexibleNullability)
    }

    internal fun flexibleNullabilityAnnotationConstructorCall(): IrConstructorCall? =
        flexibleNullabilityAnnotationSymbol?.toConstructorCall()

    private val extensionFunctionTypeAnnotationSymbol by lazy {
        annotationSymbolById(StandardClassIds.Annotations.ExtensionFunctionType)
    }

    internal fun extensionFunctionTypeAnnotationConstructorCall(): IrConstructorCall? =
        extensionFunctionTypeAnnotationSymbol?.toConstructorCall()

    private val rawTypeAnnotationSymbol by lazy {
        annotationSymbolById(StandardClassIds.Annotations.RawTypeAnnotation)
    }

    internal fun rawTypeAnnotationConstructorCall(): IrConstructorCall? =
        rawTypeAnnotationSymbol?.toConstructorCall()

    private fun annotationSymbolById(id: ClassId): IrClassSymbol? =
        provider?.getClassSymbolById(id) ?: session.symbolProvider.getClassLikeSymbolByClassId(id)?.toSymbol(
            ConversionTypeContext.DEFAULT
        ) as? IrClassSymbol

    private fun IrClassSymbol.toConstructorCall(): IrConstructorCallImpl =
        IrConstructorCallImpl.fromSymbolOwner(defaultType, owner.declarations.firstIsInstance<IrConstructor>().symbol)
}
