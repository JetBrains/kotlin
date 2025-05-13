import kotlin.reflect.KFunction1

// MODULE: lib
// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    internal inline fun internalInlineFunction(): KFunction1<String, A> = ::A
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("OK").s
}
