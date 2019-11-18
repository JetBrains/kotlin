// !LANGUAGE: +FunctionInterfaceConversion

fun interface Foo {
    fun invoke(): String
}

class A : Foo {
    override fun invoke(): String {
        return "OK"
    }
}

fun box(): String {
    return A().invoke()
}
