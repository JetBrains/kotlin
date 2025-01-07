// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

interface Foo

private class FooImpl : Foo

private inline fun privateMethod(): Foo = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>FooImpl()<!>

internal inline fun internalMethod(): Foo {
    return privateMethod()
}