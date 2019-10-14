// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

class A {
    fun foo(i: A) {}

    fun baz(i: A) {}
}

class B {
    fun foo(s: B) {}
    fun foo(c: Char) {}

    fun baz(s: B) {}
}

fun <T> bar(f: (T) -> Unit): T = TODO()

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myWith<!>(A()) {
        val t1 = bar(::foo)
        <!DEBUG_INFO_EXPRESSION_TYPE("A")!>t1<!>

        val t2 = bar(::baz)
        <!DEBUG_INFO_EXPRESSION_TYPE("A")!>t2<!>

        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myWith<!>(B()) {
            val a: A = bar(::foo)
            val b: B = bar(::foo)

            val t3 = bar(::baz)
            <!DEBUG_INFO_EXPRESSION_TYPE("B")!>t3<!>

            <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>foo<!>)
        }
    }
}

inline fun <T, R> myWith(receiver: T, block: T.() -> R): R = TODO()