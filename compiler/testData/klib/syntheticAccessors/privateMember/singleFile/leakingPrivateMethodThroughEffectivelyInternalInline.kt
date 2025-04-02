internal class A {
    private fun privateMethod() = "OK"

    inline fun inlineFunction() = privateMethod()
}

fun box(): String {
    return A().inlineFunction()
}
