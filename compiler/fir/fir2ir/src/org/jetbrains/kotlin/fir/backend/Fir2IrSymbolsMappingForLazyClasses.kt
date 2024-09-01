/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Some later stages of the Fir2Ir process (namely building fake overrides)
 * need to remap some symbols.
 *
 * This remapping should be back-propagated to storages, so they would return
 * remapped values. This is important for Lazy Classes and potentially important
 * for plugins.
 *
 * This class serves this purpose. These parts can register a remapping,
 * and corresponding symbols would be lazily updated in lazy declarations
 * and in return values of storages.
 */
class Fir2IrSymbolsMappingForLazyClasses {
    private var remapper: SymbolRemapper? = null

    /**
     * This value can be used for fast check if something changed since last time.
     * It should have new unique values on each change of class' state.
     *
     * Typical usage should cache the mapping result, and invalidate cache if and only if this value has changed.
     */
    var generation: Int = 0
        private set

    fun remapFunctionSymbol(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        return remapper?.getReferencedSimpleFunction(symbol) ?: symbol
    }

    fun remapPropertySymbol(symbol: IrPropertySymbol): IrPropertySymbol {
        return remapper?.getReferencedProperty(symbol) ?: symbol
    }

    @RequiresOptIn
    annotation class SymbolRemapperInternals

    @SymbolRemapperInternals
    fun initializeRemapper(remapper: SymbolRemapper) {
        require(this.remapper == null) { "Remapper already initialized"}
        this.remapper = remapper
        generation++
    }

    @SymbolRemapperInternals
    fun unregisterRemapper() {
        requireNotNull(remapper) { "Remapper was already disposed" }
        remapper = null
        generation++
    }
}

internal class MappedLazyVar<T>(
    val lock: IrLock,
    initializer: () -> T,
    val map: Fir2IrSymbolsMappingForLazyClasses,
    val mapperFun: Fir2IrSymbolsMappingForLazyClasses.(T) -> T
) : ReadWriteProperty<Any?, T> {
    private val lazy = lazyVar(lock, initializer)

    @Volatile
    private var lastSeenGeneration: Int = -1

    override fun toString(): String = lazy.toString()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return synchronized(lock) {
            if (lastSeenGeneration != map.generation) {
                lazy.setValue(thisRef, property, map.mapperFun(lazy.getValue(thisRef, property)))
                lastSeenGeneration = map.generation
            }
            lastSeenGeneration = map.generation
            lazy.getValue(thisRef, property)
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(lock) {
            lazy.setValue(thisRef, property, value)
            lastSeenGeneration = map.generation
        }
    }
}

fun <T> Fir2IrSymbolsMappingForLazyClasses.lazyMappedVar(
    lock: IrLock,
    initializer: () -> T,
    mapFunction: Fir2IrSymbolsMappingForLazyClasses.(T) -> T
): ReadWriteProperty<Any?, T> {
    return MappedLazyVar(lock, initializer, this, mapFunction)
}

fun Fir2IrSymbolsMappingForLazyClasses.lazyMappedFunctionListVar(
    lock: IrLock,
    initializer: () -> List<IrSimpleFunctionSymbol>
): ReadWriteProperty<Any?, List<IrSimpleFunctionSymbol>> = lazyMappedVar(lock, initializer) { list ->
    list.map { remapFunctionSymbol(it) }
}

fun Fir2IrSymbolsMappingForLazyClasses.lazyMappedPropertyListVar(
    lock: IrLock,
    initializer: () -> List<IrPropertySymbol>
): ReadWriteProperty<Any?, List<IrPropertySymbol>> = lazyMappedVar(lock, initializer) { list ->
    list.map { remapPropertySymbol(it) }
}
