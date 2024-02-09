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
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myWith<!>(A()) <!CANNOT_INFER_PARAMETER_TYPE!>{
        val t1 = bar(::foo)

        val t2 = bar(::baz)

        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myWith<!>(B()) <!CANNOT_INFER_PARAMETER_TYPE!>{
            val a: A = bar(::foo)
            val b: B = bar(::foo)

            val t3 = bar(::baz)

            <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>)
        }<!>
    }<!>
}

inline fun <T, R> myWith(receiver: T, block: T.() -> R): R = TODO()
