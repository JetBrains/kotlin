public open class A<T> {
    fun foo(x: T) = "O"
    fun foo(x: A<T>) = "K"
}

// Shoudt not be reported CONFLICTING_INHERITED_JVM_DECLARATIONS
class B : A<A<String>>()

fun box(): String {
    val x: A<String> = A()
    val y: A<A<String>> = A()
    val b = B()

    return b.foo(x) + b.foo(y)
}
