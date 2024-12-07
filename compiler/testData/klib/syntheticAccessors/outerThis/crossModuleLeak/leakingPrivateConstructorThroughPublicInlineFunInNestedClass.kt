// KT-72862: No constructor found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: Outer.kt
class Outer private constructor(val s: String) {
    constructor() : this("")

    class Nested {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        public inline fun copy(s: String) = Outer(s)
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return Outer.Nested().copy("OK").s
}
