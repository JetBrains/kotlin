// RUN_PIPELINE_TILL: FRONTEND
fun <T: <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Int.() -> String<!>> foo() {}

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T: <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Int.() -> String<!><!>> bar = fun (x: Int): String { return x.toString() }

class A<T> where T : <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Double.(Int) -> Unit<!>

interface B<T, U : <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>T.() -> Unit<!>>

fun <T: <!UNSUPPORTED_FEATURE!>context(Int)<!> () -> String> foo2() {}

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T: <!UNSUPPORTED_FEATURE!>context(Int)<!> () -> String<!>> bar2 = fun (x: Int): String { return x.toString() }

class A2<T> where T : <!UNSUPPORTED_FEATURE!>context(Double)<!> (Int) -> Unit

interface B2<T, U : <!UNSUPPORTED_FEATURE!>context(T)<!> () -> Unit>

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, functionDeclaration, functionalType, interfaceDeclaration,
nullableType, propertyDeclaration, typeConstraint, typeParameter, typeWithContext, typeWithExtension */
