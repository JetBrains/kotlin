external enum class Foo : Enum<Foo> { A, B }

fun box(a: Any, b: Any): Boolean {
    return a is Foo && b !is Foo
}