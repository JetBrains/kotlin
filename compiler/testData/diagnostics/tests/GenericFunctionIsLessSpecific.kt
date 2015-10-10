// !CHECK_TYPE

// A generic funciton is always less specific than a non-generic one
fun <T> foo(<!UNUSED_PARAMETER!>t<!> : T) : Unit {}
fun foo(<!UNUSED_PARAMETER!>i<!> : Int) : Int = 1

fun test() {
    checkSubtype<Int>(foo(1))
    checkSubtype<Unit>(foo("s"))
}
