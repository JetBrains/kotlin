// !DIAGNOSTICS: -UNUSED_PARAMETER

interface C<out T>
interface MC<T> : C<T> {
    fun addAll(x: C<T>): Boolean
    fun addAllMC(x: MC<out T>): Boolean
}

interface Open
class Derived : Open

fun <T> mc(): MC<T> = null!!
fun <T> c(): C<T> = null!!

fun foo(x: MC<out Open>) {
    x.addAll(<!TYPE_MISMATCH(C<kotlin.Nothing>; MC<out Open>)!>x<!>)
    x.addAllMC(<!TYPE_MISMATCH(MC<kotlin.Nothing>; MC<out Open>)!>x<!>)

    x.addAll(<!TYPE_MISMATCH(C<kotlin.Nothing>; MC<Open>)!>mc<Open>()<!>)
    x.addAllMC(<!TYPE_MISMATCH(MC<kotlin.Nothing>; MC<Open>)!>mc<Open>()<!>)

    x.addAll(<!TYPE_MISMATCH(C<kotlin.Nothing>; MC<Derived>)!>mc<Derived>()<!>)
    x.addAllMC(<!TYPE_MISMATCH(MC<kotlin.Nothing>; MC<Derived>)!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())
}
