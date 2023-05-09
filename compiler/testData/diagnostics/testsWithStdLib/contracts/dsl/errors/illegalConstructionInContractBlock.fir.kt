// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -NOTHING_TO_INLINE -ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS -ABSTRACT_FUNCTION_WITH_BODY -UNUSED_PARAMETER -UNUSED_VARIABLE -EXPERIMENTAL_FEATURE_WARNING

import kotlin.contracts.*

fun ifInContract(x: Any?, boolean: Boolean) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>if (boolean) {
            returns() implies (x is String)
        } else {
            returns() implies (x is Int)
        }<!>
    }
}

fun whenInContract(x: Any?, boolean: Boolean) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>when (boolean) {
            true -> returns() implies (x is String)
            else -> returns() implies (x is Int)
        }<!>
    }
}

fun forInContract(x: Any?) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>for (i in 0..1) {
            returns() implies (x is String)
        }<!>
    }
}

fun whileInContract(x: Any?) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>while (false) {
            returns() implies (x is String)
        }<!>
    }
}

fun doWhileInContract(x: Any?) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>do {
            returns() implies (x is String)
        } while (false)<!>
    }
}

fun localValInContract(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>val y: Int = 42<!>
        returns() implies (x is String)
    }<!>
}
