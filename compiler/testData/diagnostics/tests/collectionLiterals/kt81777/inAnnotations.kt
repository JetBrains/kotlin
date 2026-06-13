// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations
// WITH_STDLIB
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals

@Repeatable
annotation class AnnoString(val args: Array<String>)

@Repeatable
annotation class AnnoInt(vararg val args: Int)

@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>{}<!>]<!>)
@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }<!>]<!>)
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ it: Any -> }<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ 42 }<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> x }<!>])
fun testWithLambdas() = Unit

@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>])
@AnnoInt(*[<!ARGUMENT_TYPE_MISMATCH!>fun(x: Int) {}<!>])
@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>fun() = 42<!>]<!>)
@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>fun(x: Int) = x<!>]<!>)
fun testWithAnons() = Unit

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>::<!INAPPLICABLE_CANDIDATE!>skip<!><!>]<!>)
@AnnoString(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>::<!INAPPLICABLE_CANDIDATE!>id<!><!>]<!>)
@AnnoInt(*[::<!INAPPLICABLE_CANDIDATE!>consume<!>])
@AnnoInt(*[::<!INAPPLICABLE_CANDIDATE!>const42<!>])
fun testWithCallables() = Unit

annotation class AnnoWrong(val block: Int)

@AnnoWrong(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>[{ if (true) "a" else "b" }]<!>)
fun testWrong() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, callableReference, collectionLiteral,
functionDeclaration, integerLiteral, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, typeParameter,
vararg */
