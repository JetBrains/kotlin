// LANGUAGE: +ConditionImpliesReturnsContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

fun impliesReturn(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded
}

fun returnNotNullImplies(x: Any?): String? {
    contract {
        returnsNotNull() implies (x is String)
    }
    return x as? String
}
