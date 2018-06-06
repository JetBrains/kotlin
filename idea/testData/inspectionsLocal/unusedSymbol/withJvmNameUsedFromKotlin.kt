// WITH_RUNTIME
// PROBLEM: none

@JvmName("fooForJava")
fun <caret>foo() {}

fun test() {
    foo()
}