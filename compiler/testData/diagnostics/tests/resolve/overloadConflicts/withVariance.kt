// !WITH_NEW_INFERENCE
object X1
object X2

class A<T>

<!CONFLICTING_OVERLOADS!>fun <T1> A<T1>.foo()<!> = X1
<!CONFLICTING_OVERLOADS!>fun <T2> A<out T2>.foo()<!> = X2

fun <T> A<out T>.test() = <!CANNOT_COMPLETE_RESOLVE!>foo<!>() // TODO fix constraint system