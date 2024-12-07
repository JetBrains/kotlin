// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION
fun bar() = 42

fun main() {
    fun bar() = 239

    ::bar
}