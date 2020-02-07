//KT-1875 Safe call should be binded with receiver or this object (but not with both by default)

package kt1875

fun foo(a : Int?, b : Int.(Int)->Int) = a?.<!INAPPLICABLE_CANDIDATE!>b<!>(1) //unnecessary safe call warning

interface T {
    val f : ((i: Int) -> Unit)?
}

fun test(t: T) {
    t.<!INAPPLICABLE_CANDIDATE!>f<!>(1) //unsafe call error
    t.f?.invoke(1)
}

fun test1(t: T?) {
    t.<!UNRESOLVED_REFERENCE!>f<!>(1) // todo resolve f as value and report UNSAFE_CALL
    t?.f(1)
    t.<!INAPPLICABLE_CANDIDATE!>f<!>?.<!UNRESOLVED_REFERENCE!>invoke<!>(1)
    t?.f?.invoke(1)
}