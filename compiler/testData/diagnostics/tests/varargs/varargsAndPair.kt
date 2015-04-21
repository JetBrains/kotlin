// !CHECK_TYPE

fun foo<T: Any>(vararg <!UNUSED_PARAMETER!>ts<!>: T): T? = null

class Pair<A>(<!UNUSED_PARAMETER!>a<!>: A)

fun test() {
    val v = foo(Pair(1))
    checkSubtype<Int>(<!TYPE_MISMATCH!>v<!>) // check that it is not error type
}