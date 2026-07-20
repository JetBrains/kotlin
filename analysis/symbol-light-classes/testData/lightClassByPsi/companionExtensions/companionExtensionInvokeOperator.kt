// LANGUAGE: +CompanionBlocks +CompanionExtensions
package one

class Foo(val value: Int)

companion operator fun Foo.invoke(initial: Int): Foo = Foo(initial)
