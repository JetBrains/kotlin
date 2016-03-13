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
    x.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(C<Nothing>; MC<out Open>; MC<out Open>; public abstract fun addAll\(x: C<T>\): Boolean defined in MC)!>x<!>)
    x.addAllMC(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(MC<Nothing>; MC<out Open>; MC<out Open>; public abstract fun addAllMC\(x: MC<out T>\): Boolean defined in MC)!>x<!>)

    x.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(C<Nothing>; MC<Open>; MC<out Open>; public abstract fun addAll\(x: C<T>\): Boolean defined in MC)!>mc<Open>()<!>)
    x.addAllMC(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(MC<Nothing>; MC<Open>; MC<out Open>; public abstract fun addAllMC\(x: MC<out T>\): Boolean defined in MC)!>mc<Open>()<!>)

    x.addAll(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(C<Nothing>; MC<Derived>; MC<out Open>; public abstract fun addAll\(x: C<T>\): Boolean defined in MC)!>mc<Derived>()<!>)
    x.addAllMC(<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS(MC<Nothing>; MC<Derived>; MC<out Open>; public abstract fun addAllMC\(x: MC<out T>\): Boolean defined in MC)!>mc<Derived>()<!>)

    x.addAll(c())
    x.addAll(c<Nothing>())

    x.<!MEMBER_PROJECTED_OUT!>addAllInv<!>(mc<Open>())
    x.addAll(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
