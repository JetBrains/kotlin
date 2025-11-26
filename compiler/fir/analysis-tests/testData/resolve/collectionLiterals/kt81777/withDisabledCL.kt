// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// LANGUAGE: -CollectionLiterals

fun testWithLambdas() {
    val lam: Array<() -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ it -> }]<!>
    val withParamOfSpecifiedType: Array<(Int) -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ it: Any -> }]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ 42 }]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ x -> x }]<!>

    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{}]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ it: Any -> }]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ 42 }]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }]<!>
}

fun testWithAnons() {
    val anon: Array<() -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun() {}]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun(x: Int) {}]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun() = 42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun(x: Int) = x]<!>

    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun() {}]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun(x: Int) {}]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun() = 42]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[fun(x: Int) = x]<!>
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

fun testWithCallables() {
    val callable: Array<() -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[::skip]<!>
    val withParam: Array<(Int) -> Unit> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[::consume]<!>
    val withReturn: Array<() -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[::const42]<!>
    val withReturnAndParam: Array<(Int) -> Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[::id]<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, callableReference, collectionLiteral, functionDeclaration, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter */
