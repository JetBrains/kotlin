fun <T: <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Int.() -> String<!>> foo() {}

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T: <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Int.() -> String<!><!>> bar = fun (x: Int): String { return x.toString() }

class A<T> where T : <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>Double.(Int) -> Unit<!>

interface B<T, U : <!UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE!>T.() -> Unit<!>>