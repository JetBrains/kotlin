// FIR_IDENTICAL
// WITH_RUNTIME

fun bar() {
    listOf(1, 2, 3).<!NONE_APPLICABLE!>maxOf<!> { <!UNRESOLVED_REFERENCE!>foo<!> }
}