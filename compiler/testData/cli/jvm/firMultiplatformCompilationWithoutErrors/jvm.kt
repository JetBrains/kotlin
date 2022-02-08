actual interface A {
    actual fun foo(): String
    fun bar(): String
}

fun test(): String {
    val a = getA()
    return a.foo() + a.bar()
}
