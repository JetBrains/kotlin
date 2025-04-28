// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77078

fun foo() = object : Comparable<Int> by "" {}

fun main() {
    println(foo() < 0)
}
