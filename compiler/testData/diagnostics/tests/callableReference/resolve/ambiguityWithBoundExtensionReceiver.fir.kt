// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <R> bar(f: () -> R): R = TODO()

fun Any.foo() = 1
fun A.foo() = ""

class A {
    fun main() {
        bar(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>) checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }
}
