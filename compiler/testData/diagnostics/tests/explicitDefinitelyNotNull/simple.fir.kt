// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!): T!! = x ?: y

fun main() {
    foo<String>("", "").length
    foo<String>("", <!ARGUMENT_TYPE_MISMATCH!>null<!>).length
    foo<String?>(null, "")<!UNSAFE_CALL!>.<!>length
    foo<String?>(null, null)<!UNSAFE_CALL!>.<!>length

    foo("", "").length
    foo("", null)<!UNSAFE_CALL!>.<!>length
    foo(null, "")<!UNSAFE_CALL!>.<!>length
}
