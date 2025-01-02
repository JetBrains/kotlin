// For now the checker is working before inlining. The test should be corrected after KT-71416.
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

interface Foo

private class FooImpl : Foo

private inline fun privateMethod(): Foo = FooImpl()

internal inline fun internalMethod(): Foo {
    return privateMethod()
}