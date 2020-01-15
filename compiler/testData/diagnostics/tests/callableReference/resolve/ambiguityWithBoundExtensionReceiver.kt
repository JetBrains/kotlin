// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <R> bar(f: () -> R): R = TODO()

fun Any.foo() = 1
fun A.foo() = ""

class A {
    fun main() {
        bar(::foo) checkType { _<String>() }
    }
}
