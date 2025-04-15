// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76781

fun <T : Any> foo(): MutableList<T> = TODO()

fun main() {
    val x: MutableList<String?> = <!TYPE_MISMATCH!>foo()<!>
}