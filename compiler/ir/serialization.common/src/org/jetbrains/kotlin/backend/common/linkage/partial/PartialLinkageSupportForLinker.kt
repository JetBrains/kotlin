/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

interface PartialLinkageSupportForLinker {
    val isEnabled: Boolean

    /**
     * Fast check to determine if the given [declaration] should be skipped from the partial linkage point of view.
     *
     * This check is typically used to avoid processing and patching declarations that came from stdlib or were generated
     * on the fly by the compiler itself and this way are automatically supposed to be correct.
     *
     * Note: There is no need to call [shouldBeSkipped] prior to [exploreClassifiersInInlineLazyIrFunction] and
     * [generateStubsAndPatchUsages] functions. These function do the same check internally in more optimal way.
     */
    fun shouldBeSkipped(declaration: IrDeclaration): Boolean

    /**
     * Enqueue the file for processing by the partial linkage engine.
     *
     * Note: Enqueueing the file does not mean any declarations in this file are also enqueued.
     * For enqueueing declarations it is always required to make explicit [enqueueDeclaration] calls.
     */
    fun enqueueFile(file: IrFile)

    /**
     * Enqueue the declaration for processing by the partial linkage engine.
     */
    fun enqueueDeclaration(declaration: IrDeclaration)

    /**
     * For general use in IR linker.
     *
     * Note: Those classifiers that were detected as partially linked are excluded from the fake overrides generation
     * to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
     */
    fun exploreClassifiers(fakeOverrideBuilder: IrLinkerFakeOverrideProvider)

    /**
     * For local use only in inline lazy-IR functions.
     *
     * Such functions are fully deserialized when e.g. a cache is generated for a Kotlin/Native library given that the function itself
     * is from another library. The rest of IR from another library remains lazy meantime.
     */
    fun exploreClassifiersInInlineLazyIrFunction(function: IrFunction)

    /**
     * Generate stubs for the remaining unbound symbols.
     * Process (and patch, if necessary) all declarations previously scheduled for processing via [enqueueDeclaration].
     */
    fun generateStubsAndPatchUsages(symbolTable: SymbolTable)

    /**
     * Collect all symbols which were stubbed
     */
    fun collectAllStubbedSymbols(): Set<IrSymbol>

    companion object {
        val DISABLED = object : PartialLinkageSupportForLinker {
            override val isEnabled get() = false
            override fun shouldBeSkipped(declaration: IrDeclaration) = true
            override fun enqueueFile(file: IrFile) = Unit
            override fun enqueueDeclaration(declaration: IrDeclaration) = Unit
            override fun exploreClassifiers(fakeOverrideBuilder: IrLinkerFakeOverrideProvider) = Unit
            override fun exploreClassifiersInInlineLazyIrFunction(function: IrFunction) = Unit
            override fun generateStubsAndPatchUsages(symbolTable: SymbolTable) = Unit
            override fun collectAllStubbedSymbols(): Set<IrSymbol> = emptySet()
        }
    }
}
