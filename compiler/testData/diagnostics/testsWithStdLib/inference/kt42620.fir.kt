// SKIP_TXT

class Foo

fun main1() = when {
    else -> <!UNRESOLVED_REFERENCE!>Foo::plus<!>
}

fun main2() = if (true) Foo::minus else Foo::times

fun main3() = if (true) { Foo::minus } else { Foo::times }

fun main4() = try { Foo::minus } finally { <!UNRESOLVED_REFERENCE!>Foo::times<!> }

fun main5() = Foo::minus ?: Foo::times
