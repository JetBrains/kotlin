// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

@Repeatable
annotation class AnnoString(val args: Array<String>)

@Repeatable
annotation class AnnoInt(vararg val args: Int)

@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>{}<!>]<!>)
@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>{ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> }<!>]<!>)
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ it: Any -> }<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ 42 }<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>x<!> -> x }<!>])
fun testWithLambdas() {
}

@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>fun(x: Int) {}<!>])
@AnnoString(<!ARGUMENT_TYPE_MISMATCH, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>fun() = 42<!>]<!>)
@AnnoString(<!ARGUMENT_TYPE_MISMATCH, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>fun(x: Int) = x<!>]<!>)
fun testWithAnons() {
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

@AnnoString(<!ARGUMENT_TYPE_MISMATCH!>[::skip]<!>)
@AnnoString(<!ARGUMENT_TYPE_MISMATCH!>[::<!CANNOT_INFER_PARAMETER_TYPE!>id<!>]<!>)
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>::<!CANNOT_INFER_PARAMETER_TYPE!>consume<!><!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>::const42<!>])
fun testWithCallables() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, callableReference, collectionLiteral,
functionDeclaration, integerLiteral, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, typeParameter,
vararg */
