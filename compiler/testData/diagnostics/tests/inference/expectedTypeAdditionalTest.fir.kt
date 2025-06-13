// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeFromCast

fun foo() = 1

fun <T> foo() = foo() <!UNCHECKED_CAST!>as T<!>

fun <T> foo2(): T = TODO()

val test = <!CANNOT_INFER_PARAMETER_TYPE!>foo2<!>().plus("") <!USELESS_CAST!>as String<!>

fun <T> T.bar() = this
val barTest = "".bar() <!CAST_NEVER_SUCCEEDS!>as<!> Number

/* GENERATED_FIR_TAGS: asExpression, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
propertyDeclaration, stringLiteral, thisExpression, typeParameter */
