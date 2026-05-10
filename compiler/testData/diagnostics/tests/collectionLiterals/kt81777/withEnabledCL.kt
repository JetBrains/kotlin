// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// LANGUAGE: +CollectionLiterals

// Even with enabled collection literals and implemented fallback we might want "unresolved" collection literal in some cases,
// e.g. when standard library is missing.
// `List` is resolved to some type even without stdlib, but there is no symbol for `listOf` (needed to resolve collection literal).

fun testWithLambdas() {
    val lam: List<() -> Unit> = <!UNRESOLVED_REFERENCE!>[{}]<!>
    val withParam: List<(Int) -> Unit> = <!UNRESOLVED_REFERENCE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    val withParamOfSpecifiedType: List<(Int) -> Unit> = <!UNRESOLVED_REFERENCE!>[{ it: Any -> }]<!>
    val withReturn: List<() -> Int> = <!UNRESOLVED_REFERENCE!>[{ 42 }]<!>
    val withReturnAndParam: List<(Int) -> Int> = <!UNRESOLVED_REFERENCE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>

    <!UNRESOLVED_REFERENCE!>[{}]<!>
    <!UNRESOLVED_REFERENCE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    <!UNRESOLVED_REFERENCE!>[{ it: Any -> }]<!>
    <!UNRESOLVED_REFERENCE!>[{ 42 }]<!>
    <!UNRESOLVED_REFERENCE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>
}

fun testWithAnons() {
    val anon: List<() -> Unit> = <!UNRESOLVED_REFERENCE!>[fun() {}]<!>
    val withParam: List<(Int) -> Unit> = <!UNRESOLVED_REFERENCE!>[fun(x: Int) {}]<!>
    val withReturn: List<() -> Int> = <!UNRESOLVED_REFERENCE!>[fun() = 42]<!>
    val withReturnAndParam: List<(Int) -> Int> = <!UNRESOLVED_REFERENCE!>[fun(x: Int) = x]<!>

    <!UNRESOLVED_REFERENCE!>[fun() {}]<!>
    <!UNRESOLVED_REFERENCE!>[fun(x: Int) {}]<!>
    <!UNRESOLVED_REFERENCE!>[fun() = 42]<!>
    <!UNRESOLVED_REFERENCE!>[fun(x: Int) = x]<!>
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

fun testWithCallables() {
    val callable: List<() -> Unit> = <!UNRESOLVED_REFERENCE!>[::skip]<!>
    val withParam: List<(Int) -> Unit> = <!UNRESOLVED_REFERENCE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>consume<!>]<!>
    val withReturn: List<() -> Int> = <!UNRESOLVED_REFERENCE!>[::const42]<!>
    val withReturnAndParam: List<(Int) -> Int> = <!UNRESOLVED_REFERENCE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>id<!>]<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, callableReference, collectionLiteral, functionDeclaration, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter */
