// KT-72862: No constructor found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: Outer.kt
class Outer private constructor(val s: String) {
    constructor() : this("")

    inner class Inner {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        public inline fun copy(s: String) = Outer(s)
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return Outer().Inner().copy("OK").s
}
