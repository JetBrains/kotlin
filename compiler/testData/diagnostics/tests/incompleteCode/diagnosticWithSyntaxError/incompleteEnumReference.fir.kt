enum class E {
    A,
    B,
    C
}

fun foo() {
    val e = <!NO_COMPANION_OBJECT!>E<!>.<!SYNTAX!><!>
}


