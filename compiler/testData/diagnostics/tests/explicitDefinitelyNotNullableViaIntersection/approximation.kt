// FIR_IDENTICAL
// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T> foo(x: T, y: T & Any) = x!!

fun main() {
    foo<String>("", "").length
    foo<String>("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo<String?>(null, "").length
    foo<String?>(null, <!NULL_FOR_NONNULL_TYPE!>null<!>).length

    foo("", "").length
    foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    foo(null, "").length
}
