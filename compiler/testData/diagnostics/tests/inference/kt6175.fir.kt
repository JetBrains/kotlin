// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T, R : Any> foo(body: (R?) -> T): T = fail()

fun test1() {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        true
    }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}


fun <T, R> bar(body: (R) -> T): T = fail()

fun test2() {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>bar<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        true
    }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        true
    }
}

fun <T, R> baz(body: (List<R>) -> T): T = fail()

fun test3() {
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!> {
        true
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!> { x ->
        true
    }
}

fun <T, R : Any> brr(body: (List<R?>) -> T): T = fail()

fun test4() {
    <!CANNOT_INFER_PARAMETER_TYPE!>brr<!> {
        true
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>brr<!> { x ->
        true
    }
}

fun fail(): Nothing = throw Exception()

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeConstraint, typeParameter */
