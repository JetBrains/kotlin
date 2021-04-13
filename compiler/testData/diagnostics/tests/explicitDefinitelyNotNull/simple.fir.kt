// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!): T!! = x ?: y

fun main() {
    foo<String>("", "").length
    foo<String>("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo<String?>(null, "")<!UNSAFE_CALL!>.<!>length
    foo<String?>(null, null)<!UNSAFE_CALL!>.<!>length

    foo("", "").length
    foo("", null)<!UNSAFE_CALL!>.<!>length
    foo(null, "")<!UNSAFE_CALL!>.<!>length
}
