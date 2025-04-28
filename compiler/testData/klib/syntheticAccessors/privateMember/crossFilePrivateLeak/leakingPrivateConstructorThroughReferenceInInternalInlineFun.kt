import kotlin.reflect.KFunction1

// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    internal inline fun internalInlineFunction(): KFunction1<String, A> = ::A
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("OK").s
}
