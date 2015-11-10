fun <T: (Int) -> String> foo() {}

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T: (kotlin.Int) -> kotlin.String<!>> bar = fun (x: Int): String { return x.toString() }

class A<T, U, V> where T : () -> Unit, U : (Int) -> Double, V : (T, U) -> U

interface B<T, U : (T) -> Unit>