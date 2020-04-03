//KT-1875 Safe call should be binded with receiver or this object (but not with both by default)

package kt1875

fun foo(a : Int?, b : Int.(Int)->Int) = a?.b(1) //unnecessary safe call warning

interface T {
    val f : ((i: Int) -> Unit)?
}

fun test(t: T) {
    t.<!UNSAFE_IMPLICIT_INVOKE_CALL!>f<!>(1) //unsafe call error
    t.f?.invoke(1)
}

fun test1(t: T?) {
    t<!UNSAFE_CALL!>.<!><!FUNCTION_EXPECTED!>f<!>(1) // todo resolve f as value and report UNSAFE_CALL
    t?.<!UNSAFE_IMPLICIT_INVOKE_CALL!>f<!>(1)
    t<!UNSAFE_CALL!>.<!>f?.invoke(1)
    t?.f?.invoke(1)
}