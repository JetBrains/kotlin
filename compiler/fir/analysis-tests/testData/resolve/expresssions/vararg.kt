// RUN_PIPELINE_TILL: BACKEND
fun foo(vararg x: String) {}

fun foo() {}

fun main() {
    foo()
    foo("!")
}
