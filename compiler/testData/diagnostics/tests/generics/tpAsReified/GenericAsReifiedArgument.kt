// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> foo() {}

inline fun <reified F> bar() {
    foo<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>Array<F><!>>()
    foo<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<F><!>>()
}

inline fun <reified E> baz(x: E) {}

fun test(x: Array<String>, y: Array<*>) {
    foo<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()
    foo<List<*>>()
    foo<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>Map<*, String><!>>()
    foo<Map<*, *>>()

    foo<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>Array<String><!>>()
    foo<Array<*>>()

    <!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>baz<!>(x)
    baz(y)
}

