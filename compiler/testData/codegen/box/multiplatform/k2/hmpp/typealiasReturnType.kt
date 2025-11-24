// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect interface A
expect interface B

interface I<out T> {
    fun get(): T
}

expect fun foo(): I<A>
expect fun bar(): I<B>

expect fun combine(a: A, b: B): String

fun baz(): String {
    val pa = foo()
    val pb = bar()

    val a = pa.get()
    val b = pb.get()

    return combine(a, b)
}

// MODULE: lib-platform()()(lib-common)

interface C {
    val t: String
}

actual typealias A = C
actual typealias B = C

class CImpl(override val t: String) : C

class CI(private val v: C) : I<C> {
    override fun get(): C = v
}

actual fun foo(): I<C> = CI(CImpl("O"))
actual fun bar(): I<C> = CI(CImpl("K"))

actual fun combine(a: C, b: C): String = a.t + b.t

fun box(): String {
    return baz()
}
