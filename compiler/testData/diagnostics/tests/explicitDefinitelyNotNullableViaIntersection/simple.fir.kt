// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T> foo(x: T, y: T & Any): T & Any = x ?: y

fun main() {
    foo<String>("", "").<!UNRESOLVED_REFERENCE!>length<!>
    foo<String>("", null).<!UNRESOLVED_REFERENCE!>length<!>
    foo<String?>(null, "").<!UNRESOLVED_REFERENCE!>length<!>
    foo<String?>(null, null).<!UNRESOLVED_REFERENCE!>length<!>

    foo("", "").<!UNRESOLVED_REFERENCE!>length<!>
    foo("", null).<!UNRESOLVED_REFERENCE!>length<!>
    foo(null, "").<!UNRESOLVED_REFERENCE!>length<!>
}
