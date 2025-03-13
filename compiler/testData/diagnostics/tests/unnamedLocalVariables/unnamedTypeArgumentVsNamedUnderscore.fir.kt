// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// WITH_STDLIB

typealias `_` = Int

fun main() {
    val x: MutableList<String> = mutableListOf<_>()
    val y: MutableList<String> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>mutableListOf<`_`>()<!>
    val z: MutableList<Int> = mutableListOf<`_`>()
}
