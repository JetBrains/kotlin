// KT-282 Nullability in extension functions and in binary calls

class Set {
    operator fun contains(<!UNUSED_PARAMETER!>x<!> : Int) : Boolean = true
}

operator fun Set?.plus(<!UNUSED_PARAMETER!>x<!> : Int) : Int = 1

operator fun Int?.contains(<!UNUSED_PARAMETER!>x<!> : Int) : Boolean = false

fun f(): Unit {
    var set : Set? = null
    val i : Int? = null
    i <!UNSAFE_OPERATOR_CALL!>+<!> 1
    set + 1
    1 <!UNSAFE_OPERATOR_CALL!>in<!> set
    1 in 2
}
