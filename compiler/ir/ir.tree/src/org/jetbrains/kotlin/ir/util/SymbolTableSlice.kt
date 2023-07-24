/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.util.PrivateForInline

abstract class SymbolTableSlice<Key, SymbolOwner, Symbol>(val lock: IrLock)
        where SymbolOwner : IrSymbolOwner,
              Symbol : IrBindableSymbol<*, SymbolOwner> {
    /**
     * With the partial linkage turned on it's hard to predict whether a newly created [IrSymbol] that is added to the [SymbolTable]
     * via one of referenceXXX() calls will or won't be bound to some declaration. The latter may happen in certain cases,
     * for example when the symbol refers from an IR expression to a non-top level declaration that was removed in newer version
     * of Kotlin library (KLIB). Unless such symbol is registered as "probably unbound" it remains invisible for the linkage process.
     *
     * The optimization that allows to reference symbols without registering them as "probably unbound" is fragile. It's better
     * to avoid calling any referenceXXX(reg = false) functions. Instead, wherever it is s suitable it is recommended to use one
     * of the appropriate declareXXX() calls.
     *
     * For the future: Consider implementing the optimization once again for the new "flat" ID signatures.
     */
    val unboundSymbols = linkedSetOf<Symbol>()

    abstract fun get(key: Key): Symbol?
    abstract fun set(key: Key, symbol: Symbol)

    @OptIn(PrivateForInline::class)
    inline fun declare(key: Key, createSymbol: () -> Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
        synchronized(lock) {
            val existing = get(key)
            val symbol = if (existing == null) {
                createSymbol().also { set(key, it) }
            } else {
                unboundSymbols.remove(existing)
                existing
            }
            return createOwnerSafe(symbol, createOwner)
        }
    }

    inline fun referenced(key: Key, orElse: () -> Symbol): Symbol {
        synchronized(lock) {
            return get(key) ?: run {
                val new = orElse()
                assert(unboundSymbols.add(new)) { "Symbol for ${new.signature} was already referenced" }
                set(key, new)
                new
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun declareIfNotExists(
        key: Key,
        createSymbol: () -> Symbol,
        createOwner: (Symbol) -> SymbolOwner,
    ): SymbolOwner {
        synchronized(lock) {
            val existing = get(key)
            val symbol = if (existing == null) {
                val new = createSymbol()
                set(key, new)
                new
            } else {
                if (existing.isBound) {
                    unboundSymbols.remove(existing)
                    return existing.owner
                }
                existing
            }
            return createOwnerSafe(symbol, createOwner)
        }
    }

    @PrivateForInline
    @PublishedApi
    internal inline fun createOwnerSafe(symbol: Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
        val owner = createOwner(symbol)
        require(symbol.isBound)
        require(symbol.owner === owner) {
            "Attempt to rebind an IR symbol or to re-create its owner: old owner ${symbol.owner.render()}, new owner ${owner.render()}"
        }
        return owner
    }

    class Flat<Key, SymbolOwner, Symbol>(lock: IrLock, val symbolFilter: (Symbol) -> Boolean = { true }) : SymbolTableSlice<Key, SymbolOwner, Symbol>(lock)
            where SymbolOwner : IrSymbolOwner, Symbol : IrBindableSymbol<*, SymbolOwner> {
        private val signatureToSymbol = hashMapOf<Key, Symbol>()

        override fun get(key: Key): Symbol? {
            return signatureToSymbol[key]
        }

        override fun set(key: Key, symbol: Symbol) {
            if (symbolFilter(symbol)) {
                signatureToSymbol[key] = symbol
            }
        }

        @SymbolTableInternals
        internal fun forEachSymbol(block: (IrSymbol) -> Unit) {
            signatureToSymbol.forEach { (_, symbol) -> block(symbol) }
        }
    }

    class Scoped<Key, SymbolOwner, Symbol>(lock: IrLock) : SymbolTableSlice<Key, SymbolOwner, Symbol>(lock)
            where SymbolOwner : IrSymbolOwner, Symbol : IrBindableSymbol<*, SymbolOwner> {

        @OptIn(PrivateForInline::class)
        override fun set(key: Key, symbol: Symbol) {
            val scope = currentScope ?: noScope()
            scope[key] = symbol
        }

        @PublishedApi
        @PrivateForInline
        internal var currentScope: SliceScope<Key, Symbol>? = null
            private set

        @OptIn(PrivateForInline::class)
        override fun get(key: Key): Symbol? {
            return currentScope?.get(key)
        }

        @OptIn(PrivateForInline::class)
        inline fun declareLocal(key: Key, createSymbol: () -> Symbol, createOwner: (Symbol) -> SymbolOwner): SymbolOwner {
            val scope = currentScope ?: noScope()
            val symbol = scope.getLocal(key) ?: createSymbol().also { scope[key] = it }
            return createOwnerSafe(symbol, createOwner)
        }

        @OptIn(PrivateForInline::class)
        fun introduceLocal(descriptor: Key, symbol: Symbol) {
            val scope = currentScope ?: noScope()
            scope[descriptor]?.let { error("$descriptor is already bound to $it") }
            scope[descriptor] = symbol
        }

        @OptIn(PrivateForInline::class)
        fun enterScope(owner: IrSymbol) {
            currentScope = SliceScope(owner, currentScope)
        }

        @OptIn(PrivateForInline::class)
        fun leaveScope(owner: IrSymbol) {
            currentScope?.owner.let {
                require(it == owner) { "Unexpected leaveScope: owner=$owner, currentScope.owner=$it" }
            }

            currentScope = currentScope?.parent

            if (currentScope != null && unboundSymbols.isNotEmpty()) {
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                error("Local scope contains unbound symbols: ${unboundSymbols.joinToString { it.descriptor.toString() }}")
            }
        }

        @OptIn(PrivateForInline::class)
        fun dump(): String {
            return currentScope?.dump() ?: "<none>"
        }

        @PublishedApi
        @PrivateForInline
        internal fun noScope(): Nothing = error("No active scope")

        @PublishedApi
        @PrivateForInline
        internal class SliceScope<Key, Symbol>(val owner: IrSymbol, val parent: SliceScope<Key, Symbol>?) {
            private val signatureToSymbol = hashMapOf<Key, Symbol>()

            operator fun get(descriptor: Key): Symbol? {
                return signatureToSymbol[descriptor] ?: parent?.get(descriptor)
            }

            fun getLocal(descriptor: Key): Symbol? {
                return signatureToSymbol[descriptor]
            }

            operator fun set(descriptor: Key, symbol: Symbol) {
                signatureToSymbol[descriptor] = symbol
            }

            fun dumpTo(stringBuilder: StringBuilder): StringBuilder {
                return stringBuilder.also {
                    it.append("owner=")
                    it.append(owner)
                    it.append("; ")
                    signatureToSymbol.keys.joinTo(prefix = "[", postfix = "]", buffer = it)
                    it.append('\n')
                    parent?.dumpTo(it)
                }
            }

            fun dump(): String = dumpTo(StringBuilder()).toString()
        }
    }
}
