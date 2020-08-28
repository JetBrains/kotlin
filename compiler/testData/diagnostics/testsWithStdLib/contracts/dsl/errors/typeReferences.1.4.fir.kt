// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <reified T> referToReifiedGeneric(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is T)
    }<!>
}

class Generic<T> {
    fun referToCaptured(x: Any?) {
        <!WRONG_IMPLIES_CONDITION!>contract {
            returns() implies (x is T)
        }<!>
    }
}

fun referToSubstituted(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is Generic<String>)
    }<!>
}

fun referToSubstitutedWithStar(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is Generic<*>)
    }<!>
}

typealias GenericString = Generic<String>
typealias FunctionalType = () -> Unit
typealias SimpleType = Int

fun referToAliasedGeneric(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is GenericString)
    }<!>
}

fun referToAliasedFunctionType(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is FunctionalType)
    }<!>
}

fun referToAliasedSimpleType(x: Any?) {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x is SimpleType)
    }<!>
}