// MODULE: lib
// FILE: A.kt
class A {
    internal inline fun internalInlineMethodA() = 21.privateExtensionMethod()
    internal inline fun internalInlineMethodB() = 21.privateExtensionVar

    private fun Int.privateExtensionMethod() = this
    private val Int.privateExtensionVar
        get() = this
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += internalInlineMethodA()
        result += internalInlineMethodB()
    }
    if (result != 42) return result.toString()
    return "OK"
}
