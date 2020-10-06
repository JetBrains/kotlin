// ERROR_POLICY: SEMANTIC

// FILE: t.kt

fun <reified T> bar(t: T) = t

fun <reified T> qux() = T::class

fun foo(): String {
    return bar<String>("OK")
}

fun dec() { qux() }

// FILE: b.kt

fun box(): String {
    try {
        dec()
    } catch (e: Throwable /*js ReferenceError*/) {
        return foo()
    }
}