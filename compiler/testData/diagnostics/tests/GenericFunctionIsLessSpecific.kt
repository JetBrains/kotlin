// A generic funciton is always less specific than a non-generic one
fun foo<T>(<!UNUSED_PARAMETER!>t<!> : T) : Unit {}
fun foo(<!UNUSED_PARAMETER!>i<!> : Int) : Int = 1

fun test() {
    foo(1) : Int
    foo("s") : Unit
}
