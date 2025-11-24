// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect interface A
expect interface B

expect class E<T> where T : A, T : B {
    fun foo(v: T): T
}

fun <T> useE(
    combined: E<T>,
    v: T
): T where T : A, T : B {
    return combined.foo(v)
}


// MODULE: lib-platform()()(lib-common)

interface IA {
    val a: String
}

interface IB {
    val b: String
}

class Impl(
    override val a: String,
    override val b: String
) : IA, IB

actual typealias A = IA
actual typealias B = IB

actual class E<T> where T : IA, T : IB {
    actual fun foo(v: T): T = v
}

fun box(): String {
    val combined = E<Impl>()
    val v = Impl("O", "K")
    val res = useE(combined, v)
    return res.a + res.b
}
