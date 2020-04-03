class A {
    val lambda: () -> Unit = TODO()
    val memberInvoke: B = TODO()
    val extensionInvoke: C = TODO()
}

class B {
    operator fun invoke() {}
}

class C
operator fun C.invoke() {}

fun test(a: A?) {
    a?.lambda()
    a?.memberInvoke()
    a?.extensionInvoke()
}
