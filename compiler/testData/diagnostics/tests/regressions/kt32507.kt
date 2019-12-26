// !LANGUAGE: +NewInference

fun foo(bar: Any?): Int {
    bar as String?
    <!DEBUG_INFO_SMARTCAST!>bar<!> ?: throw IllegalStateException()
    return <!DEBUG_INFO_SMARTCAST!>bar<!>.length
}