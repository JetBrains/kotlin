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
    myWith(A()) {
        val t1 = bar(::foo)
        <!DEBUG_INFO_EXPRESSION_TYPE("A")!>t1<!>

        val t2 = bar(::baz)
        <!DEBUG_INFO_EXPRESSION_TYPE("A")!>t2<!>

        myWith(B()) {
            val a: A = bar(::foo)
            val b: B = bar(::foo)

            val t3 = bar(::baz)
            <!DEBUG_INFO_EXPRESSION_TYPE("B")!>t3<!>

            <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER, IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>bar<!>(::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)
        }
    }
}

inline fun <T, R> myWith(receiver: T, block: T.() -> R): R = TODO()