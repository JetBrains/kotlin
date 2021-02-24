// ERROR_POLICY: SEMANTIC

// MODULE: lib
// FILE: t.kt

fun bar<T>(a: T): T = a

var storage = ""

fun foo() {
    storage += bar("O")
    storage += bar<Any, String, Number>("K")
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    foo()
    return storage
}