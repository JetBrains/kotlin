// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

public class X: I1 {
    override fun <A> f(a: A): A = a
}

public interface I1 : I2

public expect interface I2 {
    public fun <A> f(a: A): A
}

public class Y<B>: I3<B> {
    override fun f(b: B): B = b
}

public interface I3<B> : I4<B>

public expect interface I4<B> {
    public fun f(b: B): B
}

fun box() = X().f("O") + Y<String>().f("K")

// MODULE: platform()()(common)
// FILE: platform.kt

public actual interface I2 {
    public actual fun <A> f(a: A): A
}

public actual interface I4<B> {
    public actual fun f(b: B): B
}
