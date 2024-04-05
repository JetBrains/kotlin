/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.Fir2IrSpecialSymbolProvider
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId

class Fir2IrJvmSpecialAnnotationSymbolProvider(irFactory: IrFactory) : Fir2IrSpecialSymbolProvider() {
    private val provider = JvmIrSpecialAnnotationSymbolProvider(irFactory)

    override fun getClassSymbolById(id: ClassId): IrClassSymbol? =
        provider.getClassSymbolById(id)
}
