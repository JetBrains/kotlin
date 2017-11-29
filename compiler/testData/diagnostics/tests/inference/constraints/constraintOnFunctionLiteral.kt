// !WITH_NEW_INFERENCE
package c

import java.util.ArrayList

fun Array<Int>.toIntArray(): IntArray = this.<!NI;TYPE_MISMATCH!><!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>mapTo<!>(<!NI;TYPE_MISMATCH!>IntArray(size)<!>, {<!NI;TYPE_MISMATCH!>it<!>})<!>

fun Array<Int>.toArrayList(): ArrayList<Int> = this.mapTo(ArrayList<Int>(size), {it})

public fun <T, R, C: MutableCollection<in R>> Array<out T>.mapTo(result: C, transform : (T) -> R) : C =
        throw Exception("$result $transform")

