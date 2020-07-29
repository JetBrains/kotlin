// !WITH_NEW_INFERENCE
// KT-282 Nullability in extension functions and in binary calls

class Set {
    operator fun contains(x : Int) : Boolean = true
}

operator fun Set?.plus(x : Int) : Int = 1

operator fun Int?.contains(x : Int) : Boolean = false

fun f(): Unit {
    var set : Set? = null
    val i : Int? = null
    i <!NONE_APPLICABLE!>+<!> 1
    set + 1
    1 <!INAPPLICABLE_CANDIDATE!>in<!> set
    1 in 2
}
