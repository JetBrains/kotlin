fun <T, R> baz(body: (List<R>) -> T): T = TODO()

fun test3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> {
        true
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> { x ->
        true
    }
}