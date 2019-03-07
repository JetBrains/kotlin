/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.name.Name

interface FirScope {
    @Deprecated(
        "obsolete",
        replaceWith = ReplaceWith("processClassifiersByNameWithAction(name, position) { if (processor()) ProcessorAction.NEXT else ProcessorAction.STOP }.next()")
    )
    fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean = true

    fun processFunctionsByName(
        name: Name,
        processor: (ConeFunctionSymbol) -> ProcessorAction
    ): ProcessorAction = NEXT

    fun processPropertiesByName(
        name: Name,
        processor: (ConeVariableSymbol) -> ProcessorAction
    ): ProcessorAction = NEXT
}


inline fun FirScope.processClassifiersByNameWithAction(
    name: Name,
    position: FirPosition,
    crossinline processor: (ConeClassifierSymbol) -> ProcessorAction
): ProcessorAction {
    val result = processClassifiersByName(name, position) {
        processor(it).next()
    }
    return if (result) NEXT else STOP
}

enum class FirPosition(val allowTypeParameters: Boolean = true) {
    SUPER_TYPE_OR_EXPANSION(allowTypeParameters = false),
    OTHER
}

enum class ProcessorAction {
    STOP,
    NEXT;

    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
        }
    }

    fun next() = this == NEXT
}
