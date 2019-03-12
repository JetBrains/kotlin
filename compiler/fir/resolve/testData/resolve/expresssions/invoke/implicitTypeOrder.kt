
class A {
    fun bar() = foo()

    fun invoke() = this
}

fun create() = A()

val foo = create()