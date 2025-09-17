// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-81115

val l1: context(String) (Int) -> String = fun(x: String, y) = ""
val l2: context(String) (Int) -> String = <!INITIALIZER_TYPE_MISMATCH!>{ x: String, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!> -> "" }<!>

val l3: context(String) Double.(Int) -> String = fun(x: String, y, z: Int) = ""
val l4: context(String) Double.(Int) -> String = <!INITIALIZER_TYPE_MISMATCH!>{ x: String, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!>, z: Int -> "" }<!>

val w1: context(String) Double.(Int, Int) -> String = <!INITIALIZER_TYPE_MISMATCH!>fun(x: String, y, z: Int) = ""<!>
val w2: context(String) Double.(Int, Int) -> String = <!INITIALIZER_TYPE_MISMATCH!>{ x: String, y, z: Int -> "" }<!>

val w3: context(String) Double.() -> String = <!INITIALIZER_TYPE_MISMATCH!>fun(x: String, y, z: Int) = ""<!>
val w4: context(String) Double.() -> String = <!INITIALIZER_TYPE_MISMATCH!>{ x: String, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!>, z: Int -> "" }<!>

/* GENERATED_FIR_TAGS: anonymousFunction, functionalType, lambdaLiteral, propertyDeclaration, stringLiteral,
typeWithContext, typeWithExtension */
