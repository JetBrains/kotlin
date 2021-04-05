// !WITH_NEW_INFERENCE
package typeConstructorMismatch
//+JDK

import java.util.*

fun test(set: Set<String>) {
    elemAndList("2", <!ARGUMENT_TYPE_MISMATCH!>set<!>)

    "".elemAndListWithReceiver("", <!ARGUMENT_TYPE_MISMATCH!>set<!>)
}

fun <R> elemAndList(r: R, t: List<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, t: List<R>): R = r
