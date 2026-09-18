error class Foo

fun foo(
    a: String | Foo,
    b: String? | Foo,
    c: (String | Foo)?,
    d: List<String | Foo>,
){ }
