// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77078

fun foo() = object : Comparable<Int> by <!TYPE_MISMATCH!>""<!> {}

fun main() {
    println(foo() < 0)
}
