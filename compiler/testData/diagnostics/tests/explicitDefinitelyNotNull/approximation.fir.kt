// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!) = x!!

fun main() {
    foo<String>("", "").length
    foo<String>("", <!ARGUMENT_TYPE_MISMATCH!>null<!>).length
    foo<String?>(null, "").length
    foo<String?>(null, null).length

    foo("", "").length
    foo("", null).length
    foo(null, "").length
}
