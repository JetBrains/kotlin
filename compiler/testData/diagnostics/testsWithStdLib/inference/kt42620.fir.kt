// SKIP_TXT

class Foo

fun main1() = when {
    else -> <!UNRESOLVED_REFERENCE!>Foo::plus<!>
}

fun main2() = if (true) <!UNRESOLVED_REFERENCE!>Foo::minus<!> else <!UNRESOLVED_REFERENCE!>Foo::times<!>

fun main3() = if (true) { <!UNRESOLVED_REFERENCE!>Foo::minus<!> } else { <!UNRESOLVED_REFERENCE!>Foo::times<!> }

fun main4() = try { <!UNRESOLVED_REFERENCE!>Foo::minus<!> } finally { <!UNRESOLVED_REFERENCE!>Foo::times<!> }

fun main5() = <!UNRESOLVED_REFERENCE!>Foo::minus<!> ?: <!UNRESOLVED_REFERENCE!>Foo::times<!>
