// MODULE: lib
// FILE: A.kt

class A {
    val a: Number
        private field = 1

    val b: Number
        internal field = 2

    val c: Number
        <!WRONG_MODIFIER_TARGET!>protected<!> field = 3

    val d: Number
        <!WRONG_MODIFIER_TARGET!>public<!> field = 5

    fun rest() {
        val aI = A().a <!NONE_APPLICABLE!>+<!> 10
        val bI = A().b <!NONE_APPLICABLE!>+<!> 20
        val cI = A().c <!NONE_APPLICABLE!>+<!> 30
        val dI = A().d <!NONE_APPLICABLE!>+<!> 40
    }
}

fun test() {
    val aA = A().a <!NONE_APPLICABLE!>+<!> 10
    val bA = A().b <!NONE_APPLICABLE!>+<!> 20
    val cA = A().c <!NONE_APPLICABLE!>+<!> 30
    val dA = A().d <!NONE_APPLICABLE!>+<!> 40
}

// MODULE: main(lib)
// FILE: B.kt

fun main() {
    val aB = A().a <!NONE_APPLICABLE!>+<!> 10
    val bB = A().b <!NONE_APPLICABLE!>+<!> 20
    val cB = A().c <!NONE_APPLICABLE!>+<!> 30
    val dB = A().d <!NONE_APPLICABLE!>+<!> 40
}
