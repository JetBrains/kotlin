/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrMemberWithContainerSource
import org.jetbrains.kotlin.ir.util.SymbolTable

interface Fir2IrExtensions {
    val irNeedsDeserialization: Boolean

    fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource, components: Fir2IrComponents): IrClass?
    fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean
    fun registerDeclarations(symbolTable: SymbolTable)
    fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue?

    object Default : Fir2IrExtensions {
        override val irNeedsDeserialization: Boolean
            get() = false

        override fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource, components: Fir2IrComponents): IrClass? = null
        override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean = false
        override fun registerDeclarations(symbolTable: SymbolTable) {}
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope) = null
    }
}