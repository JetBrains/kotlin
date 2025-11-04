// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +ConditionImpliesReturnsContracts

@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun decodeFake(encoded: String?): String? {
    contract {
        (encoded == null) implies (returnsNotNull())
    }
    return if (encoded == null) "hello" else null
}

fun decodeIfString(encoded: Any): String? {
    contract {
        (encoded is String) implies (returnsNotNull())
    }
    return when (encoded) {
        is String -> encoded + "a"
        else -> null
    }
}

fun decodeNotNumber(encoded: Any): String? {
    contract {
        (encoded !is Number) implies (returnsNotNull())
    }
    return when (encoded) {
        is Number -> null
        else -> encoded.toString()
    }
}