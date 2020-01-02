// !WITH_NEW_INFERENCE
package typeConstructorMismatch
//+JDK

import java.util.*

fun test(set: Set<String>) {
    <!INAPPLICABLE_CANDIDATE!>elemAndList<!>("2", set)

    "".<!INAPPLICABLE_CANDIDATE!>elemAndListWithReceiver<!>("", set)
}

fun <R> elemAndList(r: R, t: List<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, t: List<R>): R = r
