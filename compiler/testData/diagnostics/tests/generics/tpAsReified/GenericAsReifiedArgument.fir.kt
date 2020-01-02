// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> foo() {}

inline fun <reified F> bar() {
    foo<Array<F>>()
    foo<List<F>>()
}

inline fun <reified E> baz(x: E) {}

fun test(x: Array<String>, y: Array<*>) {
    foo<List<String>>()
    foo<List<*>>()
    foo<Map<*, String>>()
    foo<Map<*, *>>()

    foo<Array<String>>()
    foo<Array<*>>()

    baz(x)
    baz(y)
}

