// FIR_IDENTICAL
fun <T, R> baz(body: (List<R>) -> T): T = TODO()

fun test3() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> {
        true
    }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> { x ->
        true
    }
}