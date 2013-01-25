class A {
    public val f : ()->String = {"OK"}
}

fun box(): String {
    val a = A()
    return a.f() // does not work: (in runtime) ClassCastException: A cannot be cast to jet.Function0
}
