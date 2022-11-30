// SKIP_TXT

class Foo

fun main1() = when {
    else -> Foo::<!UNRESOLVED_REFERENCE!>plus<!>
}

fun main2() = <!INAPPLICABLE_CANDIDATE!>if (true) Foo::<!UNRESOLVED_REFERENCE!>minus<!> else Foo::<!UNRESOLVED_REFERENCE!>times<!><!>

fun main3() = <!INAPPLICABLE_CANDIDATE!>if (true) { Foo::<!UNRESOLVED_REFERENCE!>minus<!> } else { Foo::<!UNRESOLVED_REFERENCE!>times<!> }<!>

fun main4() = <!INAPPLICABLE_CANDIDATE!>try { Foo::<!UNRESOLVED_REFERENCE!>minus<!> } finally { Foo::<!UNRESOLVED_REFERENCE!>times<!> }<!>

fun main5() = Foo::<!UNRESOLVED_REFERENCE!>minus<!> <!INAPPLICABLE_CANDIDATE!>?:<!> Foo::<!UNRESOLVED_REFERENCE!>times<!>
