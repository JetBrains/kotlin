// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION

inline fun <reified T> foo() {
    <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::toString<!>
}

inline fun <reified T> f(): T = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    f()

    val a: A = f()
    f<A>()

    val b: Int = f()
    f<Int>()

    val с: A = id(f())
}
