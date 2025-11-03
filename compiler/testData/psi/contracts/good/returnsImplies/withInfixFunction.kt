// LANGUAGE: +ConditionImpliesReturnsContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

infix fun String?.shl(x: String): String? {
    contract {
        (this@shl != null) implies (returnsNotNull())
    }
    return this
}
