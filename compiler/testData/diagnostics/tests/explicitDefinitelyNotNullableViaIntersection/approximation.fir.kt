// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T> foo(x: T, y: T & Any) = x!!

fun main() {
    foo<String>("", "").length
    foo<String>("", null).length
    foo<String?>(null, "").length
    foo<String?>(null, null).length

    foo("", "").length
    foo("", null).length
    foo(null, "").<!UNRESOLVED_REFERENCE!>length<!>
}
