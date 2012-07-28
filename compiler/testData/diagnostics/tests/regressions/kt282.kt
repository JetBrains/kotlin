// KT-282 Nullability in extension functions and in binary calls

class Set {
    fun contains(<!UNUSED_PARAMETER!>x<!> : Int) : Boolean = true
}

fun Set?.plus(<!UNUSED_PARAMETER!>x<!> : Int) : Int = 1

fun Int?.contains(<!UNUSED_PARAMETER!>x<!> : Int) : Boolean = false

fun f(): Unit {
    var set : Set? = null
    val i : Int? = null
    i <!UNSAFE_INFIX_CALL!>+<!> 1
    set + 1
    1 <!UNSAFE_INFIX_CALL!>in<!> set
    1 in 2
}
