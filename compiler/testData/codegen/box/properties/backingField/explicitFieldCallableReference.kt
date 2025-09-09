// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// WITH_REFLECT
// ISSUE: KT-80378

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
