package a

//KT-2234 'period!!' has type Int?

class Pair<A, B>(val a: A, val b: B)

fun main(args : Array<String>) {
    val d : Long = 1
    val period : Int? = null
    if (period != null) Pair(d, period<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> : Int) else Pair(d, 1)
    if (period != null) Pair(d, <!DEBUG_INFO_SMARTCAST!>period<!> : Int) else Pair(d, 1)
}

fun foo() {
    val x : Int? = 3
    if (x != null)  {
        val <!UNUSED_VARIABLE!>u<!> = x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> : Int
        val y = <!DEBUG_INFO_SMARTCAST!>x<!> : Int
        val <!UNUSED_VARIABLE!>z<!> : Int = y
    }
}
