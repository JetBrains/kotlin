fun take(arg: Any) {}

fun <T> foo(a: A, t: T) {} // 1
fun <T> foo(b: B, t : T) {} // 2
fun foo(i: Int) {} // 3

class A
class B

fun box(): String {
    val ref1 = take(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>) // error before 1.6.20; ok, resolved to (3) since 1.6.20
    return "OK"
}
