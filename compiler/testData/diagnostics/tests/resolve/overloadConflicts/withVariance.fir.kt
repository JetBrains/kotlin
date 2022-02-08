object X1
object X2

class A<T>

fun <T1> A<T1>.foo() = X1
fun <T2> A<out T2>.foo() = X2

fun <T> A<out T>.test() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>() // TODO fix constraint system
