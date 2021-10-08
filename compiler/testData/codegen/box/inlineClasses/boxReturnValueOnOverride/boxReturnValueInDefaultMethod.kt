// IGNORE_BACKEND: JVM
// WITH_RUNTIME

interface X<T> {
    operator fun plus(n: Int) : T
    fun next(): T = this + 1
}

@JvmInline
value class A(val value: Int) : X<A> {
    override operator fun plus(n: Int) = A(value + n)
}

fun box(): String {
    val res = A(1).next()
    return if (res.value == 2) "OK" else "FAIL $res"
}
