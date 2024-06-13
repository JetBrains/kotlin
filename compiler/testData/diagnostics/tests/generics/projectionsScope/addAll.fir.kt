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
    x.addAll(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    x.addAllMC(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    x.addAll(<!ARGUMENT_TYPE_MISMATCH!>mc<Open>()<!>)
    x.addAllMC(<!ARGUMENT_TYPE_MISMATCH!>mc<Open>()<!>)

    x.addAll(<!ARGUMENT_TYPE_MISMATCH!>mc<Derived>()<!>)
    x.addAllMC(<!ARGUMENT_TYPE_MISMATCH!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.addAllInv(<!ARGUMENT_TYPE_MISMATCH!>mc<Open>()<!>)
    x.addAll(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
}
