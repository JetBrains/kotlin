external enum class Foo1 { A, B }
external enum class Foo2 { A, B }

fun box(a: Any) = when (a) {
    <!CANNOT_CHECK_FOR_EXTERNAL_ENUM!>is Foo1<!> -> 0
    <!CANNOT_CHECK_FOR_EXTERNAL_ENUM!>!is Foo2<!> -> 1
    else -> 2
}