// FIR_IDENTICAL
package conflictingSubstitutions
//+JDK

import java.util.*

fun <R> elemAndList(r: R, t: MutableList<R>): R = r
fun <R> R.elemAndListWithReceiver(r: R, t: MutableList<R>): R = r

fun test() {
    val s = elemAndList(11, list("72"))

    val u = 11.elemAndListWithReceiver(4, list("7"))
}

fun <T> list(value: T) : ArrayList<T> {
    val list = ArrayList<T>()
    list.add(value)
    return list
}
