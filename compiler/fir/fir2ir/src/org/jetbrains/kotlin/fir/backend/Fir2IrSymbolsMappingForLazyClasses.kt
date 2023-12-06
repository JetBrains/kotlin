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
    private var symbolMap = mutableListOf<SymbolRemapper>()

    /**
     * This value can be used for fast check if something changed since last time.
     * It should have new unique values on each change of class' state.
     *
     * As for now, only adds are supported, so the size of the list works as this unique number.
     *
     * Typical usage should cache the mapping result, and invalidate cache if and only if this value has changed.
     */
    val generation: Int get() = symbolMap.size

    fun remapFunctionSymbol(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        return symbolMap.fold(symbol) { s, m -> m.getReferencedSimpleFunction(s) }
    }

    fun remapPropertySymbol(symbol: IrPropertySymbol): IrPropertySymbol {
        return symbolMap.fold(symbol) { s, m -> m.getReferencedProperty(s) }
    }

    @RequiresOptIn
    annotation class SymbolRemapperInternals

    @SymbolRemapperInternals
    fun initializeSymbolMap(map: SymbolRemapper) {
        symbolMap.add(map)
    }
}

internal class MappedLazyVar<T>(
    val lock: IrLock,
    initializer: () -> T,
    val map: Fir2IrSymbolsMappingForLazyClasses,
    val mapperFun: Fir2IrSymbolsMappingForLazyClasses.(T) -> T
) : ReadWriteProperty<Any?, T> {
    private val lazy = lazyVar(lock, initializer)
    @Volatile private var lastSeenGeneration: Int = -1

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