// SKIP_TXT

class Foo

fun main1() = when {
    else -> Foo::<!UNRESOLVED_REFERENCE!>plus<!>
}

fun main2() = if (true) Foo::minus else Foo::times

fun main3() = if (true) { Foo::minus } else { Foo::times }

fun main4() = try { Foo::minus } finally { Foo::<!UNRESOLVED_REFERENCE!>times<!> }

fun main5() = Foo::minus ?: Foo::times
