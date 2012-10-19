fun foo<T: Any>(vararg <!UNUSED_PARAMETER!>ts<!>: T): T? = null

class Pair<A>(a: A)

fun test() {
    val v = foo(Pair(1))
    <!TYPE_MISMATCH!>v<!>: Int // check that it is not error type
}