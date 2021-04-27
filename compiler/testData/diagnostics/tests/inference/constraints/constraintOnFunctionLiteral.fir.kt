// !WITH_NEW_INFERENCE
package c

import java.util.ArrayList

fun Array<Int>.toIntArray(): IntArray = <!RETURN_TYPE_MISMATCH!>this.mapTo(<!ARGUMENT_TYPE_MISMATCH!>IntArray(size)<!>, {it})<!>

fun Array<Int>.toArrayList(): ArrayList<Int> = this.mapTo(ArrayList<Int>(size), {it})

public fun <T, R, C: MutableCollection<in R>> Array<out T>.mapTo(result: C, transform : (T) -> R) : C =
        throw Exception("$result $transform")
