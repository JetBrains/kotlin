// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// IGNORE_INLINER: IR
// ^ outer this accesors are not generated correctly in jvm with ir inliner

// FILE: Outer.kt
class Outer private constructor(val s: String) {
    constructor() : this("")

    inner class Inner {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        public inline fun copy(s: String) = Outer(s)
    }
}

// FILE: main.kt
fun box(): String {
    return Outer().Inner().copy("OK").s
}
