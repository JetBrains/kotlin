//KT-580 Type inference failed

package whats.the.difference

import java.util.*

fun iarray(vararg a : String) = a // BUG

fun main() {
    val vals = iarray("789", "678", "567")
    val diffs = ArrayList<Int>()
    for (i in vals.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>indices<!>) {
        for (j in i..vals.lastIndex())  // Type inference failed
             diffs.add(vals[i].length - vals[j].length)
        for (j in i..vals.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>lastIndex<!>)  // Type inference failed
             diffs.add(vals[i].length - vals[j].length)
    }
}

fun <T> Array<T>.lastIndex() = size - 1
val <T> Array<T>.lastIndex : Int get() = size - 1
val <T> Array<T>.indices : IntRange get() = IntRange(0, <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>lastIndex<!>)
