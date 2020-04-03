public open class A<T> {
    fun foo(x: T) = "O"
    fun foo(x: A<T>) = "K"
}

interface C<E> {
    fun foo(x: E): String
    fun foo(x: A<E>): String
}

interface D {
    fun foo(x: A<A<String>>): String
}

// Shoudt not be reported CONFLICTING_INHERITED_JVM_DECLARATIONS
class B : A<A<String>>(), C<A<String>>, D

fun box(): String {
    val x: A<String> = A()
    val y: A<A<String>> = A()

    val b = B()
    val bResult = b.foo(x) + b.foo(y)
    if (bResult != "OK") return "fail 1: $bResult"

    val c: C<A<String>> = B()
    val cResult = c.foo(x) + c.foo(y)
    if (cResult != "OK") return "fail 2: $cResult"

    val d: D = B()

    if (d.foo(y) != "K") return "fail 3"

    return "OK"
}
