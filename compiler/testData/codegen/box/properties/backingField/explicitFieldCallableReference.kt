// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB
// WITH_REFLECT

class A {
    val city: List<String>
        field = mutableListOf<String>()

    fun foo() = ::city.invoke()
}

fun bar() {
    val a = A()
    a::city.invoke()
}

fun box() = "OK".also {
    A().foo()
    bar()
}
