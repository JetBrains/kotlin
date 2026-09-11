// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// LANGUAGE: -CollectionLiterals

fun testWithLambdas() {
    val lam: Array<() -> Unit> = <!UNSUPPORTED_FEATURE!>[{}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_FEATURE!>[{ it -> }]<!>
    val withParamOfSpecifiedType: Array<(Int) -> Unit> = <!UNSUPPORTED_FEATURE!>[{ it: Any -> }]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_FEATURE!>[{ 42 }]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_FEATURE!>[{ x -> x }]<!>

    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[{}]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[{ it: Any -> }]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[{ 42 }]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>
}

fun testWithAnons() {
    val anon: Array<() -> Unit> = <!UNSUPPORTED_FEATURE!>[fun() {}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_FEATURE!>[fun(x: Int) {}]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_FEATURE!>[fun() = 42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_FEATURE!>[fun(x: Int) = x]<!>

    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[fun() {}]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[fun(x: Int) {}]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[fun() = 42]<!>
    <!UNRESOLVED_REFERENCE, UNSUPPORTED_FEATURE!>[fun(x: Int) = x]<!>
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

fun testWithCallables() {
    val callable: Array<() -> Unit> = <!UNSUPPORTED_FEATURE!>[::skip]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_FEATURE!>[::consume]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_FEATURE!>[::const42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_FEATURE!>[::id]<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, callableReference, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, typeParameter */
