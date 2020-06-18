// !DIAGNOSTICS: -UNUSED_PARAMETER

fun interface IFoo {
    fun foo(i: Int)
}

fun interface IFoo2 : IFoo

object A

operator fun A.get(i: IFoo) = 1
operator fun A.set(i: IFoo, newValue: Int) {}

fun withVararg(vararg xs: Int) = 42

fun test1() {
    <!UNRESOLVED_REFERENCE!><!INAPPLICABLE_CANDIDATE!>A[::withVararg]<!> += 1<!>
}