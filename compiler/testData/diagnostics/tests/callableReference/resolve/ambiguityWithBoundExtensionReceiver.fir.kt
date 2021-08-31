// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <R> bar(f: () -> R): R = TODO()

fun Any.foo() = 1
fun A.foo() = ""

class A {
    fun main() {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>) <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<String>() }
    }
}
