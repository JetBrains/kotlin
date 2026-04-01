// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150

annotation class LamInt(val e: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[{}]<!>)
annotation class LamIntArray(val e: IntArray = [<!ARGUMENT_TYPE_MISMATCH!>{}<!>])
annotation class LamArrayString(val e: Array<String> = [<!ARGUMENT_TYPE_MISMATCH!>{}<!>])
annotation class LamCorrect(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Unit><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[{}]<!>)
annotation class LamCorrectWithParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Unit><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[{}]<!>)
annotation class LamCorrectWithReturn(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Int><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[{ 42 }]<!>)
annotation class LamCorrectWithReturnAndParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Int><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[{ it -> it }]<!>)

annotation class AnonInt(val e: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[fun() {}]<!>)
annotation class AnonIntArray(val e: IntArray = [<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>])
annotation class AnonArrayString(val e: Array<String> = [<!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun() {}<!>])
annotation class AnonCorrect(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Unit><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[fun() {}]<!>)
annotation class AnonCorrectWithParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Unit><!> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>[fun(x: Int) {}]<!>)
annotation class AnonCorrectWithReturn(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Int><!> = [<!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun() { return <!RETURN_TYPE_MISMATCH!>42<!> }<!>])
annotation class AnonCorrectWithReturnAndParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Int><!> = [<!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun(it: Int) { return <!RETURN_TYPE_MISMATCH!>it<!> }<!>])

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

annotation class RefInt(val e: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> [::skip])
annotation class RefIntArray(val e: IntArray = [<!ARGUMENT_TYPE_MISMATCH!>::skip<!>])
annotation class RefArrayString(val e: Array<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> [::skip])
annotation class RefCorrect(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Unit><!> = [::skip])
annotation class RefCorrectWithParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Unit><!> = [::consume])
annotation class RefCorrectWithReturn(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<() -> Int><!> = [::const42])
annotation class RefCorrectWithReturnAndParam(val e: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<(Int) -> Int><!> = [::id])

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, callableReference, collectionLiteral,
functionDeclaration, integerLiteral, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
