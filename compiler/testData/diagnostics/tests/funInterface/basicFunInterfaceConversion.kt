// FIR_IDENTICAL
// !LANGUAGE: +FunctionalInterfaceConversion +SamConversionPerArgument
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun interface Foo {
    fun invoke()
}

fun foo(f: Foo) {}

fun test() {
    foo {}
}
