// !WITH_NEW_INFERENCE
package typeConstructorMismatch
//+JDK

import java.util.*

fun test(set: Set<String>) {
    <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>elemAndList<!>("2", <!TYPE_MISMATCH!>set<!>)

    "".<!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>elemAndListWithReceiver<!>("", <!TYPE_MISMATCH!>set<!>)
}

fun <R> elemAndList(r: R, <!UNUSED_PARAMETER!>t<!>: List<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, <!UNUSED_PARAMETER!>t<!>: List<R>): R = r
