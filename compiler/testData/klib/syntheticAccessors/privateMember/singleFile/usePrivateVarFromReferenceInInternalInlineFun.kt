// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class A {
    private var privateVar = 22

    internal inline fun internalInlineFunction() = ::privateVar
}

fun box(): String {
    var result = 0
    A().run {
        result += internalInlineFunction().get()
        internalInlineFunction().set(20)
        result += internalInlineFunction().get()
    }
    if (result != 42) return result.toString()
    return "OK"
}
