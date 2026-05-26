// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo

companion fun Foo.greet(): String = "Hi"

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionFunctionKt.class[greet]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionFunctionKt.class[greet]
