// WITH_STDLIB

fun bar() {
    listOf(1, 2, 3).<!NONE_APPLICABLE!>maxOf<!> <!UNRESOLVED_REFERENCE!>{ <!UNRESOLVED_REFERENCE!>foo<!> }<!>
}