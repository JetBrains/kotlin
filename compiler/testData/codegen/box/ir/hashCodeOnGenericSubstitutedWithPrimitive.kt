// JVM_TARGET: 1.8

class A<T> {
    fun id(x: T): T = x
}

fun foo(f: A<Boolean>): Int =
    f.id(true).hashCode()

fun box(): String =
    if (foo(A<Boolean>()) == true.hashCode()) "OK" else "Fail"
