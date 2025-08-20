import kotlin.reflect.KFunction1

// MODULE: lib
// FILE: A.kt
class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(): KFunction1<String, Inner> = ::Inner
    }
}

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String {
    return A().Inner().internalInlineMethod().invoke("OK").s
}
