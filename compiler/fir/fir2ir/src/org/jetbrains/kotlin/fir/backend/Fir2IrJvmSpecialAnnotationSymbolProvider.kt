/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class Fir2IrJvmSpecialAnnotationSymbolProvider(private val provider: Fir2IrSpecialSymbolProvider) : IrSpecialAnnotationsProvider {
    override val enhancedNullabilityAnnotationCall by lazy {
        StandardClassIds.Annotations.EnhancedNullability.toConstructorCall()
    }

    override val flexibleNullabilityAnnotationCall by lazy {
        StandardClassIds.Annotations.FlexibleNullability.toConstructorCall()
    }

    override val flexibleMutabilityAnnotationCall by lazy {
        StandardClassIds.Annotations.FlexibleMutability.toConstructorCall()
    }

    override val flexibleArrayElementVarianceAnnotationCall by lazy {
        StandardClassIds.Annotations.FlexibleArrayElementVariance.toConstructorCall()
    }

    override val rawTypeAnnotationCall by lazy {
        StandardClassIds.Annotations.RawTypeAnnotation.toConstructorCall()
    }

    private fun ClassId.toConstructorCall(): IrConstructorCallImpl {
        val classSymbol = provider.getClassSymbolById(this)!!

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val constructorSymbol = classSymbol.owner.declarations.firstIsInstance<IrConstructor>().symbol
        return IrConstructorCallImpl.fromSymbolOwner(classSymbol.defaultType, constructorSymbol)
    }
}
