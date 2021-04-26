// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!) = x!!

fun main() {
    foo<String>("", "").length
    foo<String>("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo<String?>(null, "").length
    foo<String?>(null, null).length

    foo("", "").length
    foo("", null).length
    foo(null, "").length
}
