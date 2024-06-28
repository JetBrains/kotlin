// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <reified T> referToReifiedGeneric(x: Any?) {
    contract {
        returns() implies (x is T)
    }
}

class Generic<T> {
    fun referToCaptured(x: Any?) {
        contract {
            returns() implies (x is <!CANNOT_CHECK_FOR_ERASED, ERROR_IN_CONTRACT_DESCRIPTION!>T<!>)
        }
    }
}

fun referToSubstituted(x: Any?) {
    <!ERROR_IN_CONTRACT_DESCRIPTION("Error in contract description")!>contract<!> {
        returns() implies (x is <!CANNOT_CHECK_FOR_ERASED!>Generic<String><!>)
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
    <!ERROR_IN_CONTRACT_DESCRIPTION("Error in contract description")!>contract<!> {
        returns() implies (x is <!CANNOT_CHECK_FOR_ERASED!>GenericString<!>)
    }
}

fun referToAliasedFunctionType(x: Any?) {
    <!ERROR_IN_CONTRACT_DESCRIPTION("Error in contract description")!>contract<!> {
        returns() implies (x is <!CANNOT_CHECK_FOR_ERASED!>FunctionalType<!>)
    }
}

fun referToAliasedSimpleType(x: Any?) {
    contract {
        returns() implies (x is SimpleType)
    }
}