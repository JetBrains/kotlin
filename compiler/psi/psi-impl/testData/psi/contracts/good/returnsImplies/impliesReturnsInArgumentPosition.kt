// LANGUAGE: +ConditionImpliesReturnsContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79277, KT-79526
import kotlin.contracts.*

fun notNullIfNotNull(encoded: String?): Boolean? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded != null
}

fun String?.notNullIfNull(): Boolean? {
    contract {
        (this@notNullIfNull == null) implies returnsNotNull()
    }
    return this !== null
}

val String?.notNullIfNull: Boolean?
    get() {
        contract {
            (this@notNullIfNull == null) implies returnsNotNull()
        }
        return this !== null
    }
