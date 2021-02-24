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
    x.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>x<!>)
    x.addAllMC(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>x<!>)

    x.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>mc<Open>()<!>)
    x.addAllMC(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>mc<Open>()<!>)

    x.addAll(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>mc<Derived>()<!>)
    x.addAllMC(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS{OI}!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.<!MEMBER_PROJECTED_OUT{OI}!>addAllInv<!>(<!TYPE_MISMATCH{NI}!>mc<Open>()<!>)
    x.addAll(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
