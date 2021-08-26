interface A<T, V, U>
interface B<U, T, W> : A<T, Int, U>
typealias C<X> = B<X, X, Any?>

fun a(x: A<String, Int, String>) = x as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>
fun b(x: A<String, Int, Any?>) = x as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>
fun c(x: A<String, Any?, String>) = x as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>
fun d(x: A<String, Int, Any?>) = x as <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!>
