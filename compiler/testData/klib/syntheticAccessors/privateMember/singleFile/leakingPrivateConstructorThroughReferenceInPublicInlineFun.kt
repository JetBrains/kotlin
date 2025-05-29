// Can be replaced with ignore after KT-69941

import kotlin.reflect.KFunction1

class A private constructor(val s: String) {
    constructor(): this("")

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction(): KFunction1<String, A> = ::A
}

fun box(): String {
    return A().publicInlineFunction().invoke("OK").s
}
