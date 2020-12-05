// SKIP_TXT

class Foo

fun main1() = when {
    else -> Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>plus<!>
}

fun main2() = if (true) Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>minus<!> else Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>times<!>

fun main3() = if (true) { Foo::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>minus<!> } else { Foo::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>times<!> }

fun main4() = try { Foo::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>minus<!> } finally { <!UNUSED_EXPRESSION!>Foo::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>times<!><!> }

fun main5() = Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>minus<!> ?: Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>times<!>
