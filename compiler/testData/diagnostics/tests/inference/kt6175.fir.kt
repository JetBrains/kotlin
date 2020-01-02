// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T, R : Any> foo(body: (R?) -> T): T = fail()

fun test1() {
    foo {
        true
    }
    foo { x ->
        true
    }
}


fun <T, R> bar(body: (R) -> T): T = fail()

fun test2() {
    bar {
        true
    }
    bar { x ->
        true
    }
}

fun <T, R> baz(body: (List<R>) -> T): T = fail()

fun test3() {
    baz {
        true
    }
    baz { x ->
        true
    }
}

fun <T, R : Any> brr(body: (List<R?>) -> T): T = fail()

fun test4() {
    brr {
        true
    }
    brr { x ->
        true
    }
}

fun fail(): Nothing = throw Exception()