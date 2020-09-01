// ERROR_POLICY: SEMANTIC

// FILE: t.kt

fun bar<T>(a: T): T = a

var storage = ""

fun foo() {
    storage += bar("O")
    storage += bar<Any, String, Number>("K")
}

// FILE: b.kt

fun box(): String {
    foo()
    return storage
}