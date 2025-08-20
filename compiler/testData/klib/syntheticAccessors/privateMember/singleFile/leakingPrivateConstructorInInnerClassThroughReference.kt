import kotlin.reflect.KFunction1

class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(): KFunction1<String, Inner> = ::Inner
    }
}

fun box(): String {
    return A().Inner().internalInlineMethod().invoke("OK").s
}
