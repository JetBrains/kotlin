// FIR_IDENTICAL
// SKIP_TXT
interface A
interface B<X : A>
interface C<E : A, F : B<E>>

fun foo1(c: C<out A, out B<*>>) {}

fun foo2(c: C<*, B<*>>) {}
fun <T : B<*>> foo3(c: C<*, T>) {}
fun <T : B<*>> foo4(c: C<out A, T>) {}
