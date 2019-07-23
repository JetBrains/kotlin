
class A {
    fun bar() = foo() // should resolve to invoke

    fun invoke() = this
}

fun create() = A()

val foo = create()