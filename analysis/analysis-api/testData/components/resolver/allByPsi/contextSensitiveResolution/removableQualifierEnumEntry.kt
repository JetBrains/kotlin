// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

enum class Foo { BAR }

fun expectsFoo(foo: Foo) {}

fun usage(): Foo {
    expectsFoo(Foo.BAR)
    return Foo.BAR
}
