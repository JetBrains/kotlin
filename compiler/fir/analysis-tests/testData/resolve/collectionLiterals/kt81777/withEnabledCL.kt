// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// LANGUAGE: +CollectionLiterals

// Even with enabled collection literals and implemented fallback we might want "unresolved" collection literal in some cases,
// e.g. when standard library is missing.

fun testWithLambdas() {
    val lam: Array<() -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    val withParamOfSpecifiedType: Array<(Int) -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ it: Any -> }]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ 42 }]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>

    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{}]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ it: Any -> }]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ 42 }]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>
}

fun testWithAnons() {
    val anon: Array<() -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun() {}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun(x: Int) {}]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun() = 42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun(x: Int) = x]<!>

    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun() {}]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun(x: Int) {}]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun() = 42]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[fun(x: Int) = x]<!>
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

fun testWithCallables() {
    val callable: Array<() -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[::skip]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>consume<!>]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[::const42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>id<!>]<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, callableReference, collectionLiteral, functionDeclaration, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter */
