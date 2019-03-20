/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import kotlin.reflect.KProperty

private var accessMode = AccessMode.ALLOWED

private enum class AccessMode(val allowed: Boolean, val log: Boolean) {
    ALLOWED(true, false),
    LOG(true, true),
    FORBIDDEN(false, false)
}

object AccessToDescriptors {
    fun allowed() = switch(AccessMode.ALLOWED)
    fun log() = switch(AccessMode.LOG)
    fun forbidden() = switch(AccessMode.FORBIDDEN)

    private fun switch(newAccessMode: AccessMode) {
        accessMode = newAccessMode
    }

    fun <R> allowed(block: () -> R) = switch(AccessMode.ALLOWED, block)
    fun <R> log(block: () -> R) = switch(AccessMode.LOG, block)
    fun <R> forbidden(block: () -> R) = switch(AccessMode.FORBIDDEN, block)

    private fun <R> switch(newAccessMode: AccessMode, block: () -> R): R {
        val curAccessMode = accessMode
        accessMode = newAccessMode
        try {
            return block()
        } finally {
            accessMode = curAccessMode
        }
    }
}
object DescriptorFromSymbolWithAccessControl {
    operator fun <D : DeclarationDescriptor> getValue(
        thisRef: IrSymbolDeclaration<IrBindableSymbol<D, *>>,
        property: KProperty<*>
    ): D = getDescriptorOrFail { thisRef.symbol.descriptor}

    operator fun <D> getValue(
        thisRef: IrSymbolOwner,
        property: KProperty<*>
    ): D = getDescriptorOrFail {
        @Suppress("UNCHECKED_CAST")
        thisRef.symbol.descriptor as D
    }

    private val traces = mutableSetOf<Array<StackTraceElement>>()

    private inline fun <D> getDescriptorOrFail(f: () -> D): D {
        if (accessMode.log) {
            val t = Thread.currentThread().stackTrace
            if (traces.add(t)) {
                System.err.println("Access to descriptor from:")
                System.err.println(t.joinToString("\n", limit = 15))
            } else {
                System.err.println("Access to descriptor with known stacktrace.")
            }
        }

        if (!accessMode.allowed) error("Using descriptors is prohibited on this stage.")

        return f()
    }
}

@Deprecated("...")
val <D : DeclarationDescriptor> IrSymbolDeclaration<IrBindableSymbol<D, *>>.descriptorWithoutAccessCheck: D
    get() = AccessToDescriptors.allowed { symbol.descriptor }

@Deprecated("...")
val IrDeclaration.descriptorWithoutAccessCheck: DeclarationDescriptor
    get() = AccessToDescriptors.allowed { descriptor }
