// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81115

val l1: String.() -> String = fun<!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(x: String)<!> = ""
val l2: String.() -> String = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x: String<!> -> "" }

val l3: String.() -> String = fun<!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(<!CANNOT_INFER_PARAMETER_TYPE!>x<!>)<!> = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
val l4: String.() -> String = { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> }

val l5: String.(Int) -> String = fun<!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>)<!> = ""
val l6: String.(Int) -> String = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> "" }

val w1: String.(Int, Int) -> String = <!TYPE_MISMATCH!>fun(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>, y) = ""<!>
val w2: String.(Int, Int) -> String = { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>, y -> "" }

val w3: String.() -> String = <!TYPE_MISMATCH!>fun<!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(x: String, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>)<!> = ""<!>
val w4: String.() -> String = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x: String, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> "" }

/* GENERATED_FIR_TAGS: anonymousFunction, functionalType, lambdaLiteral, propertyDeclaration, stringLiteral,
typeWithContext, typeWithExtension */
