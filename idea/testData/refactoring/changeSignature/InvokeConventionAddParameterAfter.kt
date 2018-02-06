class A {
    operator fun invoke(s: String, i: Int, b: Boolean) {}
}

fun usage(a: A) {
    a("", 42, false)
    A()("", 42, false)
    a.invoke("", 42, false)
}