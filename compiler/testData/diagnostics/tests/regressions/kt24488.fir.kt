// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

class Bar {
    val a: Array<String>? = null
}

fun foo(bar: Bar) = bar.a?.asIterable() ?: <!CANNOT_INFER_PARAMETER_TYPE!>emptyArray<!>()

fun <T> Array<out T>.asIterable(): Iterable<T> = TODO()

fun testFrontend() {
    val bar = Bar()
    foo(bar)
}
