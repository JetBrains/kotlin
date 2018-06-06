class A {
    operator fun get(i: Int, s: String) {}
}

fun usage(a: A) {
    a[42, ""]
    A()[42, ""]
    a.get(42, "")
}