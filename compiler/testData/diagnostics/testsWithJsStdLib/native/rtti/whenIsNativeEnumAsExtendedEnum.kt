external enum class Foo1 : Enum<Foo1> { A, B }
external enum class Foo2 : Enum<Foo2>{ A, B }

fun box(a: Any) = when (a) {
    is Foo1 -> 0
    !is Foo2 -> 1
    else -> 2
}