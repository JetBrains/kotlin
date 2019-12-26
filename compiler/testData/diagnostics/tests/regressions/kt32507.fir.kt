// !LANGUAGE: +NewInference

fun foo(bar: Any?): Int {
    bar as String?
    bar ?: throw IllegalStateException()
    return bar.length
}