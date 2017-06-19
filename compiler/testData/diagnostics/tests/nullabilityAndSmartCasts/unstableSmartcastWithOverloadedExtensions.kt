// !DIAGNOSTICS: -UNUSED_PARAMETER

class A
class B

fun A.foo(a: A) {}
fun A.foo(b: B) {}
var a: A? = null

fun smartCastInterference(b: B) {
    if (a != null) {
        <!SMARTCAST_IMPOSSIBLE!>a<!>.foo(b)
    }
}