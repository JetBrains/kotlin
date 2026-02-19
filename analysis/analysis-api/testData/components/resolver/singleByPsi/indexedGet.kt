class C {
    operator fun get(a: Int, b: String): Boolean = true
}

fun call(c: C) {
    val res = <expr>c[1, "foo"]</expr>
}