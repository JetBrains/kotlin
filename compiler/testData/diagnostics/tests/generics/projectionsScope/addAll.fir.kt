// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

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
    x.addAll(x)
    x.addAllMC(x)

    x.addAll(mc<Open>())
    x.addAllMC(mc<Open>())

    x.addAll(mc<Derived>())
    x.addAllMC(mc<Derived>())

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.addAllInv(mc<Open>())
    x.<!INAPPLICABLE_CANDIDATE!>addAll<!>(1)
}
