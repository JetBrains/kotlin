// SKIP_TXT

class Foo

fun main1() = when {
    else -> Foo::<!NONE_APPLICABLE!>plus<!>
}

fun main2() = if (true) Foo::<!NONE_APPLICABLE!>minus<!> else Foo::<!NONE_APPLICABLE!>times<!>

fun main3() = if (true) { Foo::<!NONE_APPLICABLE!>minus<!> } else { Foo::<!NONE_APPLICABLE!>times<!> }

fun main4() = try { Foo::<!NONE_APPLICABLE!>minus<!> } finally { Foo::<!NONE_APPLICABLE!>times<!> }

fun main5() = Foo::<!NONE_APPLICABLE!>minus<!> ?: Foo::<!NONE_APPLICABLE!>times<!>
