class C {
    operator fun set(a: Int, b: String, value: Boolean) {}
}

fun call(c: C) {
    <expr>c[1, "foo"]</expr> = false
}