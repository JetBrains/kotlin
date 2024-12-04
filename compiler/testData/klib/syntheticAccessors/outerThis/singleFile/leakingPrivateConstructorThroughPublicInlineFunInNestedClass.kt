// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class Outer private constructor(val s: String) {
    constructor() : this("")

    class Nested {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        public inline fun copy(s: String) = Outer(s)
    }
}

fun box(): String {
    return Outer.Nested().copy("OK").s
}
