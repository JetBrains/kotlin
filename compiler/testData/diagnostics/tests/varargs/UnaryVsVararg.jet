fun foo1(<!UNUSED_PARAMETER!>a<!> : Int) : String = "noarg"

fun foo1(<!UNUSED_PARAMETER!>a<!> : Int, vararg <!UNUSED_PARAMETER!>t<!> : Int) : String = "vararg"

fun test1() {
    foo1(1)
    val a = IntArray(0)
    foo1(1, *a)
}