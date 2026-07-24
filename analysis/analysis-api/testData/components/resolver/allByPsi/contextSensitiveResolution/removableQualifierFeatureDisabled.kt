// IDE_MODE
// LANGUAGE: -ContextSensitiveResolutionUsingExpectedType
// The hint is exposed even when the context-sensitive resolution feature is disabled.
package test

enum class Foo { BAR }

fun expectsFoo(foo: Foo) {}

fun usage(): Foo {
    expectsFoo(Foo.BAR)
    return Foo.BAR
}
