// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T, R : Any> foo(body: (R?) -> T): T = fail()

fun test1() {
    foo {
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
    foo { x ->
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
}


fun <T, R> bar(body: (R) -> T): T = fail()

fun test2() {
    bar {
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
    bar { x ->
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
}

fun <T, R> baz(body: (List<R>) -> T): T = fail()

fun test3() {
    baz {
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
    baz { x ->
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
}

fun <T, R : Any> brr(body: (List<R?>) -> T): T = fail()

fun test4() {
    brr {
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
    brr { x ->
        <!ARGUMENT_TYPE_MISMATCH!>true<!>
    }
}

fun fail(): Nothing = throw Exception()
