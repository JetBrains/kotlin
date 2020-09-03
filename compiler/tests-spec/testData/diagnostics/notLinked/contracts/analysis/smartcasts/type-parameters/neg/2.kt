// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts, kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts, type-parameters
 * NUMBER: 2
 * DESCRIPTION: Smartcasts using Returns effects with simple type checking, not-null conditions and custom condition (condition for smartcast outside contract).
 * HELPERS: contractFunctions
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41576
 */
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


sealed class Maybe<T> {
    class Value<T>(val value: T) : Maybe<T>()
}

fun test(x: Maybe<Int>, y: Any) {

    if (check(y <!NI;UNCHECKED_CAST!>as Maybe<Int><!> )) { //UNCHECKED_CAST
        val v: Int = y.<!NI;UNRESOLVED_REFERENCE!>value<!> //Maybe<Int> (smart cast from Any), value:UNRESOLVED_REFERENCE
    }
    if (check(x <!NI;USELESS_CAST!>as Maybe<Int><!> )) { // USELESS_CAST
        val v: Int = x.<!NI;UNRESOLVED_REFERENCE!>value<!> // value: UNRESOLVED_REFERENCE instead of smartcast to T
    }
}

@OptIn(ExperimentalContracts::class)
fun <T> check(x: Maybe<T>): Boolean {
    contract {
        returns() implies (x is Maybe.Value<T>)
    }
    return true
}
