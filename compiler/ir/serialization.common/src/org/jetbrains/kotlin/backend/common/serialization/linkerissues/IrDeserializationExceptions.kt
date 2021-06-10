/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.linkerissues

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class IrDeserializationException(message: String) : Exception(message) {
    override val message: String get() = super.message!!
}

class IrSymbolTypeMismatchException(
    val expected: Class<out IrSymbol>,
    val actual: IrSymbol
) : IrDeserializationException("The symbol of unexpected type encountered during IR deserialization: ${actual::class.java.simpleName}, ${actual.signature?.render() ?: actual.toString()}. ${expected.simpleName} is expected.")

class IrDisallowedErrorNode(
    clazz: Class<out IrAnnotationContainer>
) : IrDeserializationException("${clazz::class.java.simpleName} found but error nodes are not allowed.")

@OptIn(ExperimentalContracts::class)
internal inline fun <reified T : IrSymbol> checkSymbolType(symbol: IrSymbol): T {
    contract {
        returns() implies (symbol is T)
    }

    if (symbol !is T) throw IrSymbolTypeMismatchException(T::class.java, symbol) else return symbol
}

@OptIn(ExperimentalContracts::class)
internal inline fun <reified T : IrAnnotationContainer> checkErrorNodesAllowed(errorNodesAllowed: Boolean) {
    contract {
        returns() implies errorNodesAllowed
    }
    if (!errorNodesAllowed) throw IrDisallowedErrorNode(T::class.java)
}
