// RUN_PIPELINE_TILL: FRONTEND
fun <T: <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Int.() -> String<!>> foo() {}

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T: <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Int.() -> String<!><!>> bar = fun (x: Int): String { return x.toString() }

class A<T> where T : <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Double.(Int) -> Unit<!>

interface B<T, U : <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>T.() -> Unit<!>>

fun <T: <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(Int) () -> String<!>> foo2() {}

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T: <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(Int) () -> String<!><!>> bar2 = fun (x: Int): String { return x.toString() }

class A2<T> where T : <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(Double) (Int) -> Unit<!>

interface B2<T, U : <!UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(T) () -> Unit<!>>

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, functionDeclaration, functionalType, interfaceDeclaration,
nullableType, propertyDeclaration, typeConstraint, typeParameter, typeWithContext, typeWithExtension */
