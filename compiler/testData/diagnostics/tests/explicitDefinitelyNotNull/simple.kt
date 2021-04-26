// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!): T!! = x ?: y

fun main() {
    foo<String>("", "").length
    foo<String>("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo<String?>(null, "").length
    foo<String?>(null, <!NULL_FOR_NONNULL_TYPE!>null<!>).length

    foo("", "").length
    foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo(null, "").length
}
