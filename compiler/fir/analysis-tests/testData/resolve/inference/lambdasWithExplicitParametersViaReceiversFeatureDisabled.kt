// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ResolveTopLevelLambdasAsSyntheticCallArgument
// ISSUE: KT-81115

val l1: String.() -> String = fun(x: String) = ""
val l2: String.() -> String = { x: String -> "" }

val l3: String.() -> String = fun(x) = x
val l4: String.() -> String = { x -> x }

val l5: String.(Int) -> String = fun(x: String, y) = ""
val l6: String.(Int) -> String = { x: String, y -> "" }

val w1: String.(Int, Int) -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> fun(x: String, y) = ""
val w2: String.(Int, Int) -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> { x: String, y -> "" }

val w3: String.() -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> fun(x: String, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!>) = ""
val w4: String.() -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> { x: String, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!> -> "" }

/* GENERATED_FIR_TAGS: anonymousFunction, functionalType, lambdaLiteral, propertyDeclaration, stringLiteral,
typeWithContext, typeWithExtension */
