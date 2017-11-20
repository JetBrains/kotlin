// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T, R : Any> foo(body: (R?) -> T): T = fail()

fun test1() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        true
    }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}


fun <T, R> bar(body: (R) -> T): T = fail()

fun test2() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> {
        true
    }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}

fun <T, R> baz(body: (List<R>) -> T): T = fail()

fun test3() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> {
        true
    }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}

fun <T, R : Any> brr(body: (List<R?>) -> T): T = fail()

fun test4() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>brr<!> {
        true
    }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>brr<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}

fun fail(): Nothing = throw Exception()