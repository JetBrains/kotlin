// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts, kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts, type-parameters
 * NUMBER: 1
 * DESCRIPTION: Smartcasts using Returns effects with simple type checking, not-null conditions and custom condition (condition for smartcast outside contract).
 * HELPERS: contractFunctions
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41078
 */
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Maybe<T> {
    class Value<T>(val value: T) : Maybe<T>()
}

@OptIn(ExperimentalContracts::class)
fun <T> check(x: Maybe<T>): Boolean {
    contract {
        returns(true) implies (x is Maybe.Value<T>)
    }
    return true
}

fun test(x: Maybe<Int>) {
    if (x is Maybe.Value<Int>) {
        val v: Int = <!NI;DEBUG_INFO_SMARTCAST!>x<!>.value // smartcast to Int
    }
    if (check(x)) {
        val v: Int = <!NI;TYPE_MISMATCH!><!NI;DEBUG_INFO_SMARTCAST!>x<!>.value<!> // smartcast to T (!)
    }
}

