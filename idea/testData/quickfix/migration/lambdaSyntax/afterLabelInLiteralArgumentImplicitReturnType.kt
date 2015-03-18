// "Migrate lambda syntax" "true"

class A

fun foo(a: Int.(String) -> A) {}

val a = foo(fun Int.a(a: String): A {
    A()
    return A()
})