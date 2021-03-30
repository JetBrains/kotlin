// !WITH_NEW_INFERENCE
package typeConstructorMismatch
//+JDK

import java.util.*

fun test(set: Set<String>) {
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR{OI}!>elemAndList<!>("2", <!TYPE_MISMATCH!>set<!>)

    "".<!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR{OI}!>elemAndListWithReceiver<!>("", <!TYPE_MISMATCH!>set<!>)
}

fun <R> elemAndList(r: R, t: List<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, t: List<R>): R = r
