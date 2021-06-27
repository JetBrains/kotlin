/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

@JvmInline
value class BinarySymbolData(val code: Long) {
    enum class SymbolKind {
        FUNCTION_SYMBOL,
        CONSTRUCTOR_SYMBOL,
        ENUM_ENTRY_SYMBOL,
        FIELD_SYMBOL,
        VALUE_PARAMETER_SYMBOL,
        RETURNABLE_BLOCK_SYMBOL,
        CLASS_SYMBOL,
        TYPE_PARAMETER_SYMBOL,
        VARIABLE_SYMBOL,
        ANONYMOUS_INIT_SYMBOL,
        STANDALONE_FIELD_SYMBOL, // For fields without properties. WrappedFieldDescriptor, rather than WrappedPropertyDescriptor.
        RECEIVER_PARAMETER_SYMBOL, // ReceiverParameterDescriptor rather than ValueParameterDescriptor.
        PROPERTY_SYMBOL,
        LOCAL_DELEGATED_PROPERTY_SYMBOL,
        TYPEALIAS_SYMBOL,
        FILE_SYMBOL;
    }

    private fun symbolKindId(): Int = (code and 0xFF).toInt()

    val signatureId: Int get() = (code ushr 8).toInt()
    val kind: SymbolKind
        get() = SymbolKind.values()[symbolKindId()]

    companion object {
        fun encode(kind: SymbolKind, signatureId: Int): Long {
            val kindId = kind.ordinal
            return (signatureId.toLong() shl 8) or kindId.toLong()
        }

        fun decode(code: Long) = BinarySymbolData(code)
    }
}