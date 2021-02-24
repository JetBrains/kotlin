/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId

abstract class Fir2IrSpecialSymbolProvider {
    protected lateinit var components: Fir2IrComponents

    fun initComponents(components: Fir2IrComponents) {
        this.components = components
    }

    abstract fun getClassSymbolById(id: ClassId): IrClassSymbol?
}