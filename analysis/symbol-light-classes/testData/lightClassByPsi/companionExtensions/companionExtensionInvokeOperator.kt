// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo(val value: Int)

companion operator fun Foo.invoke(initial: Int): Foo = Foo(initial)

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionInvokeOperatorKt.class[invoke]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionInvokeOperatorKt.class[invoke]
