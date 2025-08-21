// LANGUAGE: -IrInlinerBeforeKlibSerialization
// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

interface Foo

private class FooImpl : Foo

private inline fun privateMethod(): Foo = FooImpl()

internal inline fun internalMethod(): Foo {
    return privateMethod()
}