// WITH_STDLIB
fun takeLong(f: () -> Long) {}
fun takeAny(f: Any) {}

fun container() {
    val long: () -> Long = fun() = 1
    val byte: () -> Byte = fun() = 1
    val nullableInt: () -> Int? = fun() = 1
    val any: () -> Any = fun() = 1
    val string: () -> CharSequence = fun() = "Hello"
    val unit: () -> Unit = fun() = 1
    val blockBody: () -> Int = fun() {}
    val blockBodyWithReturn: () -> Int = fun() { return 1 }

    takeLong(fun() = 1)
    takeAny(fun() = 1)
}
