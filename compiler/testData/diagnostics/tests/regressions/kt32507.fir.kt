// RUN_PIPELINE_TILL: BACKEND

fun foo(bar: Any?): Int {
    bar as String?
    bar ?: throw IllegalStateException()
    return bar.length
}