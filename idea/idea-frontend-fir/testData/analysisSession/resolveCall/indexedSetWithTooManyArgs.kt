class C {
    operator fun set(a: Int, b: String, value: Boolean) {}
}

fun call(c: C) {
    <expr>c[1, "foo", 3.14]</expr> = false
}