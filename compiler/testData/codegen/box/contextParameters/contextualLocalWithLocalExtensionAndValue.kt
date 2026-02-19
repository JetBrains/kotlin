// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
class A

fun localFun(): String {
    var result = ""

    fun foo(a: A): String {
        return "Value"
    }

    fun A.foo(): String {
        return "Extension"
    }

    context(a: A)
    fun foo(): String {
        return "Context"
    }

    with(A()) {
        result += foo()
    }
    result += A().foo()
    result += foo(A())
    return result
}

fun box(): String {
    if (localFun() == "ContextExtensionValue") return "OK" else return "fail"
}