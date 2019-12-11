// !CHECK_TYPE

fun <T: Any> foo(vararg ts: T): T? = null

class Pair<A>(a: A)

fun test() {
    val v = foo(Pair(1))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(v) // check that it is not error type
}