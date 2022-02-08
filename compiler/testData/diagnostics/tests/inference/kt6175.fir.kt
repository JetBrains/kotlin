// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T, R : Any> foo(body: (R?) -> T): T = fail()

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        true
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}


fun <T, R> bar(body: (R) -> T): T = fail()

fun test2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> {
        true
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}

fun <T, R> baz(body: (List<R>) -> T): T = fail()

fun test3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> {
        true
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> { x ->
        true
    }
}

fun <T, R : Any> brr(body: (List<R?>) -> T): T = fail()

fun test4() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>brr<!> {
        true
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>brr<!> { x ->
        true
    }
}

fun fail(): Nothing = throw Exception()
