// MODULE: lib
// FILE: A.kt
class A {
    inner class Inner<T> private constructor(val s: T) {
        constructor(): this("" as T)

        internal inline fun internalInlineMethod(s: T) = Inner(s)
    }
}

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String {
    return A().Inner<String>().internalInlineMethod("OK").s
}
