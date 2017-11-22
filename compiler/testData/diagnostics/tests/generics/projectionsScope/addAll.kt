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
    x.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>x<!>)
    x.addAllMC(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>x<!>)

    x.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<Open>()<!>)
    x.addAllMC(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<Open>()<!>)

    x.addAll(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<Derived>()<!>)
    x.addAllMC(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.<!OI;MEMBER_PROJECTED_OUT!>addAllInv<!>(<!NI;TYPE_MISMATCH!>mc<Open>()<!>)
    x.addAll(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
