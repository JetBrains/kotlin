// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// If this test will start to fail after KT-69666, then it can be safely removed
// MODULE: lib
// FILE: A.kt
class A<T> private constructor(val s: T) {
    constructor(): this("" as T)
    internal inline fun copy(s: String) = A(s)
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A<String>().copy("OK").s
}
