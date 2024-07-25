// FIR_IDENTICAL
// ISSUE: KT-69773
// WITH_STDLIB

import kotlin.jvm.JvmName

@JvmName("arrayVararg")
fun array(vararg elements: Int): Array<Int> = elements.toTypedArray()
fun array(elements: IntArray): Array<Int> = elements.toTypedArray()

fun main() {
    val workInKotlin19ButNotInK2 = array(elements = intArrayOf(1, 2, 3))

    val workInBoth = array(intArrayOf(1, 2, 3))
}
