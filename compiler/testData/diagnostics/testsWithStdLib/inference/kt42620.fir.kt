// SKIP_TXT

class Foo

fun main1() = when {
    else -> Foo::<!UNRESOLVED_REFERENCE!>plus<!>
}

fun main2() = if (true) Foo::<!UNRESOLVED_REFERENCE!>minus<!> else Foo::<!UNRESOLVED_REFERENCE!>times<!>

fun main3() = if (true) { Foo::<!UNRESOLVED_REFERENCE!>minus<!> } else { Foo::<!UNRESOLVED_REFERENCE!>times<!> }

fun main4() = try { Foo::<!UNRESOLVED_REFERENCE!>minus<!> } finally { Foo::<!UNRESOLVED_REFERENCE!>times<!> }

fun main5() = Foo::<!UNRESOLVED_REFERENCE!>minus<!> ?: Foo::<!UNRESOLVED_REFERENCE!>times<!>
