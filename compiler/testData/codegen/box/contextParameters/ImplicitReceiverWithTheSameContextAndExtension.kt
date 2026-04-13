// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

class A {
    fun foo(): String {
        return "not OK"
    }
}

fun box(): String {

    context(a: A)
    fun foo(): String {
        return "OK"
    }

    return with(A()) { foo() }
}
