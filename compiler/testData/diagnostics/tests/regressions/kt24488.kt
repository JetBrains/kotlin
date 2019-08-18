// !WITH_NEW_INFERENCE
// SKIP_TXT

class Bar {
    val a: Array<String>? = null
}

fun foo(bar: Bar) = bar.a?.asIterable() ?: <!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!><!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyArray<!>()<!>

fun <T> Array<out T>.asIterable(): Iterable<T> = TODO()

fun testFrontend() {
    val bar = Bar()
    foo(bar)
}