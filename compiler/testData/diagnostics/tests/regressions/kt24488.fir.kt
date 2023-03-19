// SKIP_TXT

class Bar {
    val a: Array<String>? = null
}

fun foo(bar: Bar) = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar.a?.asIterable() ?: emptyArray()<!>

fun <T> Array<out T>.asIterable(): Iterable<T> = TODO()

fun testFrontend() {
    val bar = Bar()
    foo(bar)
}
