package typeConstructorMismatch
//+JDK

import java.util.*

fun test(set: Set<String>) {
    <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>elemAndList<!>("2", <!TYPE_MISMATCH!>set<!>)

    "".<!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>elemAndListWithReceiver<!>("", <!TYPE_MISMATCH!>set<!>)
}

fun <R> elemAndList(r: R, <!UNUSED_PARAMETER!>t<!>: List<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, <!UNUSED_PARAMETER!>t<!>: List<R>): R = r
