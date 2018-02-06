class A {
    operator fun invoke(i: Int, s: String) {}
}

fun usage(a: A) {
    a(42, "")
    A()(42, "")
    a.invoke(42, "")
}