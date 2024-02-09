fun <T, R> baz(body: (List<R>) -> T): T = TODO()

fun test3() {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> {
        true
    }
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> { x ->
        true
    }
}