// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <reified T> referToReifiedGeneric(x: Any?) {
    contract {
        returns() implies (x is T)
    }
}

class Generic<T> {
    fun referToCaptured(x: Any?) {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (x is T)<!>
        }
    }
}

fun referToSubstituted(x: Any?) {
    contract {
        returns() implies (x is Generic<String>)
    }
}

fun referToSubstitutedWithStar(x: Any?) {
    contract {
        returns() implies (x is Generic<*>)
    }
}

typealias GenericString = Generic<String>
typealias FunctionalType = () -> Unit
typealias SimpleType = Int

fun referToAliasedGeneric(x: Any?) {
    contract {
        returns() implies (x is GenericString)
    }
}

fun referToAliasedFunctionType(x: Any?) {
    contract {
        returns() implies (x is FunctionalType)
    }
}

fun referToAliasedSimpleType(x: Any?) {
    contract {
        returns() implies (x is SimpleType)
    }
}
