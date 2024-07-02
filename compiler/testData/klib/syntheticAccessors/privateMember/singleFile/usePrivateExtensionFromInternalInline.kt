class A {
    internal inline fun internalInlineMethodA() = privateExtension()
    internal inline fun internalInlineMethodB() = 21.privateExtensionVar

    private val Int.privateExtensionVar
        get() = this
}

private fun A.privateExtension() = 21

fun box(): String {
    var result = 0
    A().run {
        result += internalInlineMethodA()
        result += internalInlineMethodB()
    }
    if (result != 42) return result.toString()
    return "OK"
}
