class C

fun box(): String =
    C().foo("O")

tailrec fun C.foo(
    x: String,
    f: (String) -> String = { bar(it) }
): String =
    if (x.length < 2) foo(f(x)) else x

fun C.bar(s: String): String =
    s + "K"
