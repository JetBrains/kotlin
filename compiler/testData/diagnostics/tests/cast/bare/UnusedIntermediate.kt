// FIR_IDENTICAL
interface A<T>
interface B<T, V> : A<T>
interface C<T> : B<T, Any?>

fun foo(x: A<Int>) = x as C
fun bar(x: A<Int>) = x as <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!>
