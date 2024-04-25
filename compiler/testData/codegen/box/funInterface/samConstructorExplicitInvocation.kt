// LANGUAGE: +FunctionalInterfaceConversion

fun interface S {
    fun invoke(): String
}

fun interface G<T> {
    fun foo(t: T): T
}

fun interface C: G<Char>

fun interface C2 {
    fun bar(c: Char): Char
}

fun box(): String {
    val g = G<Int> { it * 10 }
    if (g.foo(2) != 20) return "fail1"

    val g2 = G { a: Char -> a + 1 }
    if (g2.foo('a') != 'b') return "fail2"

    val c = C { it + 2 }
    if (c.foo('A') != 'C') return "fail3"

    val c2 = C2 { it + 3 }
    if (c2.bar('0') != '3') return "fail4"

    return S { "OK" }.invoke()
}
