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
        val aI = A().a + 10
        val bI = A().b + 20
        val cI = A().c <!UNRESOLVED_REFERENCE!>+<!> 30
        val dI = A().d <!UNRESOLVED_REFERENCE!>+<!> 40
    }
}

fun test() {
    val aA = A().a <!UNRESOLVED_REFERENCE!>+<!> 10
    val bA = A().b + 20
    val cA = A().c <!UNRESOLVED_REFERENCE!>+<!> 30
    val dA = A().d <!UNRESOLVED_REFERENCE!>+<!> 40
}

// MODULE: main(lib)
// FILE: B.kt

fun main() {
    val aB = A().a <!UNRESOLVED_REFERENCE!>+<!> 10
    val bB = A().b <!UNRESOLVED_REFERENCE!>+<!> 20
    val cB = A().c <!UNRESOLVED_REFERENCE!>+<!> 30
    val dB = A().d <!UNRESOLVED_REFERENCE!>+<!> 40
}
