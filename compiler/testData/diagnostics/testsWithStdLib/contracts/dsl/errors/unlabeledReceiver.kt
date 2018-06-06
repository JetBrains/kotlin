// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun Any?.foo(): Boolean {
    contract {
        returns(true) implies (<!ERROR_IN_CONTRACT_DESCRIPTION(only references to parameters are allowed. Did you miss label on <this>?)!>this<!> <!EQUALS_MISSING!>!=<!> null)
    }
    return this != null
}