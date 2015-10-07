package conflictingSubstitutions
//+JDK

import java.util.*

fun <R> elemAndList(r: R, <!UNUSED_PARAMETER!>t<!>: MutableList<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, <!UNUSED_PARAMETER!>t<!>: MutableList<R>): R = r

fun test() {
    val <!UNUSED_VARIABLE!>s<!> = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>elemAndList<!>(11, list("72"))

    val <!UNUSED_VARIABLE!>u<!> = 11.<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>elemAndListWithReceiver<!>(4, list("7"))
}

fun <T> list(value: T) : ArrayList<T> {
    val list = ArrayList<T>()
    list.add(value)
    return list
}
