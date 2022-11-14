// FIR_IDENTICAL
val x: dynamic = 23

interface I {
    fun foo(): String
}

class C : I by <!DELEGATION_BY_DYNAMIC!>x<!>

object O : I by <!DELEGATION_BY_DYNAMIC!>x<!>

fun box(): String {
    return object : I by <!DELEGATION_BY_DYNAMIC!>x<!> {}.foo()
}