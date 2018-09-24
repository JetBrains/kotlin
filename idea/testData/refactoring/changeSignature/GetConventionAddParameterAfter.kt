class A {
    operator fun get(s: String, i: Int, b: Boolean) {}
}

fun usage(a: A) {
    a["", 42, false]
    A()["", 42, false]
    a.get("", 42, false)
}