/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors

/**
 * A [SymbolTable] that de-duplicates built-ins for which multiple descriptor instances exist.
 *
 * For example, if multiple descriptors exist for the built-in `Boolean` type, the ordinary [SymbolTable] generates multiple
 * [IrClassSymbol]s, one for each descriptor instance. If the wrong `Boolean` class symbol is encountered in `JvmSharedVariableManager`,
 * an exception will occur (see KTIJ-24335).
 *
 * This class instead relies on the symbols declared in [IrBuiltIns]. Any descriptors which reference a built-in by name will be resolved
 * to that built-in, regardless of whether that descriptor's instance matches with the descriptor of the built-in.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class SymbolTableWithBuiltInsDeduplication(
    signaturer: IdSignatureComposer,
    irFactory: IrFactory,
) : SymbolTable(signaturer, irFactory) {
    /**
     * As long as [IrBuiltIns] aren't bound, the symbol table will operate like [SymbolTable], as it must be assumed that built-ins are
     * still being created.
     */
    private var irBuiltIns: IrBuiltInsOverDescriptors? = null

    fun bindIrBuiltIns(irBuiltIns: IrBuiltInsOverDescriptors) {
        if (this.irBuiltIns == null) {
            this.irBuiltIns = irBuiltIns
        } else {
            throw IllegalStateException("`irBuiltIns` have already been bound.")
        }
    }

    @ObsoleteDescriptorBasedAPI
    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
        val irBuiltIns = this.irBuiltIns ?: return super.referenceClass(descriptor)

        // We need to find out whether `descriptor` is possibly a built-in symbol before it's actually retrieved to break recursion as
        // `irBuiltIns.findClass` uses `referenceClass` recursively.
        val builtInDescriptor = irBuiltIns.findBuiltInClassDescriptor(descriptor)
        if (builtInDescriptor != null) {
            // We need to delegate to the supertype implementation here to break recursion. `findBuiltInClassDescriptor` will return
            // `descriptor` even if `descriptor` was found via `findBuiltInClassDescriptor`.
            return super.referenceClass(builtInDescriptor)
        }

        return super.referenceClass(descriptor)
    }

    private fun IrBuiltInsOverDescriptors.findBuiltInClassDescriptor(descriptor: ClassDescriptor): ClassDescriptor? {
        val packageFqName = descriptor.containingPackage() ?: return null
        return findClassDescriptor(descriptor.name, packageFqName)
    }
}
