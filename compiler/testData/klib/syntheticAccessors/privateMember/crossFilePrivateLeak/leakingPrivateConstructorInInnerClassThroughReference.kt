import kotlin.reflect.KFunction1

// FILE: A.kt
class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(): KFunction1<String, Inner> = ::Inner
    }
}

// FILE: main.kt
fun box(): String {
    return A().Inner().internalInlineMethod().invoke("OK").s
}
