// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")
    internal inline fun copy(s: String) = A(s)
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().copy("OK").s
}
