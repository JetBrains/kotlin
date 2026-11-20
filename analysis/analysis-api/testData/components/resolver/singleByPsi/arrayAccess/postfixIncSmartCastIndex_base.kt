class B {
    operator fun get(i: Any, j: String): B = this
    operator fun set(i: Any, j: String, value: B) {}
    operator fun inc(): B = this
}

fun test(b: B, x: Any) {
    <expr>b[x, x as String]</expr>++
}
