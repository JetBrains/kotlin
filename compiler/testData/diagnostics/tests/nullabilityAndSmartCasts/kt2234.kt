// CHECK_TYPE

package a

import checkSubtype

//KT-2234 'period!!' has type Int?

class Pair<A, B>(val a: A, val b: B)

fun main() {
    val d : Long = 1
    val period : Int? = null
    if (period != null) Pair(d, checkSubtype<Int>(period<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)) else Pair(d, 1)
    if (period != null) Pair(d, checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>period<!>)) else Pair(d, 1)
}

fun foo() {
    val x : Int? = 3
    if (x != null)  {
        val u = checkSubtype<Int>(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        val y = checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
        val z : Int = y
    }
}
