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
    x.<!INAPPLICABLE_CANDIDATE!>addAll<!>(x)
    x.<!INAPPLICABLE_CANDIDATE!>addAllMC<!>(x)

    x.<!INAPPLICABLE_CANDIDATE!>addAll<!>(mc<Open>())
    x.<!INAPPLICABLE_CANDIDATE!>addAllMC<!>(mc<Open>())

    x.<!INAPPLICABLE_CANDIDATE!>addAll<!>(mc<Derived>())
    x.<!INAPPLICABLE_CANDIDATE!>addAllMC<!>(mc<Derived>())

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.<!INAPPLICABLE_CANDIDATE!>addAllInv<!>(mc<Open>())
    x.<!INAPPLICABLE_CANDIDATE!>addAll<!>(1)
}
