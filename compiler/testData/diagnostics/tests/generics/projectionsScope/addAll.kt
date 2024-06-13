// DIAGNOSTICS: -UNUSED_PARAMETER

interface C<out T>
interface MC<T> : C<T> {
    fun addAll(x: C<T>): Boolean
    fun addAllMC(x: MC<out T>): Boolean
    fun addAllInv(x: MC<T>): Boolean
}

interface Open
class Derived : Open

fun <T> mc(): MC<T> = null!!
fun <T> c(): C<T> = null!!

fun foo(x: MC<out Open>) {
    x.addAll(<!TYPE_MISMATCH!>x<!>)
    x.addAllMC(<!TYPE_MISMATCH!>x<!>)

    x.addAll(<!TYPE_MISMATCH!>mc<Open>()<!>)
    x.addAllMC(<!TYPE_MISMATCH!>mc<Open>()<!>)

    x.addAll(<!TYPE_MISMATCH!>mc<Derived>()<!>)
    x.addAllMC(<!TYPE_MISMATCH!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.addAllInv(<!TYPE_MISMATCH!>mc<Open>()<!>)
    x.addAll(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
