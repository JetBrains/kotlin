// WITH_STDLIB
inline class C(val s: String)

fun f(g: () -> C): C = g()

val C.foo: C
    get() = f { this }

fun box() = C("OK").foo.s
