// COMPILATION_ERRORS
error class Foo
error class Bar

fun <T> test(
    a: Int | Foo,
    b: Int | Foo | Bar,
    c: T & Any | Foo,
    d: List<Int | Foo | Bar>,
    e: List<Int | Foo | Bar>,
    f: (Int | Foo | Bar) -> Unit,
    g: (Int | Foo | Bar).() -> Unit,
    h: Int? | Foo,
    i: (Int | Foo)?,
) {}
