// RUN_PIPELINE_TILL: FRONTEND
fun <K> foo(x: K) {}
val x1 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>{ it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }<!>
val x2 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
val x3 = foo<<!UNRESOLVED_REFERENCE!>unresolved<!>.() -> Float> <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ <!CANNOT_INFER_RECEIVER_PARAMETER_TYPE!>this<!>.<!UNRESOLVED_REFERENCE!>toFloat<!>() }<!>
val x4 = foo<(Array<<!UNRESOLVED_REFERENCE!>unresolved<!>>) -> Int> { it.size }

fun <T> bar() = foo<(T) -> String> { it.toString() }

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, propertyDeclaration,
thisExpression, typeParameter, typeWithExtension */
