class A {
    operator fun get(s: String, i: Int) {}
}

fun usage(a: A) {
    a["", 42]
    A()["", 42]
    a.get("", 42)
}