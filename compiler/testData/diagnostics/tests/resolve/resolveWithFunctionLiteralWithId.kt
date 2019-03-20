//If this test hangs, it means something is broken.
object A {
    val iii = 42
}

//inappropriate but participating in resolve functions
fun foo(s: String, a: Any) = s + a
fun foo(a: Any) = a
fun foo(i: Int) = i
fun foo(a: Any, i: Int, f: ()-> Int) = "$a$i${f()}"
fun foo(f: (Int)->Int, i: Int) = f(i)
fun foo(f: (String)->Int, s: String) = f(s)
fun foo(f: (Any)->Int, a: Any) = f(a)
fun foo(s: String, f: (String, String)->Int) = f(s, s)
//appropriate function
fun foo(i: Int, f: (Int)->Int) = f(i)

fun <T> id(t: T) = t

fun test() {
    <!UNREACHABLE_CODE!>foo(<!>1, <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id<!> { x1 ->
        <!UNREACHABLE_CODE!>foo(<!>2, <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id<!> { <!UNUSED_ANONYMOUS_PARAMETER!>x2<!> ->
            <!UNREACHABLE_CODE!>foo(<!>3, <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id<!> { <!UNUSED_ANONYMOUS_PARAMETER!>x3<!> ->
                <!UNREACHABLE_CODE!>foo(<!>4, <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id<!> { <!UNUSED_ANONYMOUS_PARAMETER!>x4<!> ->
                    <!UNREACHABLE_CODE!>foo(<!>5, <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>id<!> { <!UNUSED_ANONYMOUS_PARAMETER!>x5<!> ->
                        x1 <!UNREACHABLE_CODE!>+ x2 + x3 + x4 + x5 + A.iii<!>
                    }<!UNREACHABLE_CODE!>)<!>
                }<!UNREACHABLE_CODE!>)<!>
            }<!UNREACHABLE_CODE!>)<!>
        }<!UNREACHABLE_CODE!>)<!>
    }<!UNREACHABLE_CODE!>)<!>
}
