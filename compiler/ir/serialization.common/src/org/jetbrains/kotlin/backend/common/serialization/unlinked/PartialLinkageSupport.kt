/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable

interface PartialLinkageSupport {
    val partialLinkageEnabled: Boolean

    /**
     * For general use in IR linker.
     *
     * Note: Those classifiers that were detected as partially linked are excluded from the fake overrides generation
     * to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
     */
    fun exploreClassifiers(fakeOverrideBuilder: FakeOverrideBuilder)

    /**
     * For local use only in inline lazy-IR functions.
     *
     * Such functions are fully deserialized when e.g. a cache is generated for a Kotlin/Native library given that the function itself
     * is from another library. The rest of IR from another library remains lazy meantime.
     */
    fun exploreClassifiersInInlineLazyIrFunction(function: IrFunction)

    /**
     * Generate stubs for the remaining unbound symbols. Traverse the IR tree and patch every usage of any unbound symbol
     * to throw an appropriate IrLinkageError on access.
     */
    fun generateStubsAndPatchUsages(symbolTable: SymbolTable, roots: () -> Sequence<IrModuleFragment>)
    fun generateStubsAndPatchUsages(symbolTable: SymbolTable, root: IrDeclaration)

    companion object {
        val DISABLED = object : PartialLinkageSupport {
            override val partialLinkageEnabled get() = false
            override fun exploreClassifiers(fakeOverrideBuilder: FakeOverrideBuilder) = Unit
            override fun exploreClassifiersInInlineLazyIrFunction(function: IrFunction) = Unit
            override fun generateStubsAndPatchUsages(symbolTable: SymbolTable, roots: () -> Sequence<IrModuleFragment>) = Unit
            override fun generateStubsAndPatchUsages(symbolTable: SymbolTable, root: IrDeclaration) = Unit
        }
    }
}
