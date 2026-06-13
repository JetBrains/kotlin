// LANGUAGE: +CompanionBlocksAndExtensions
// LIBRARY_PLATFORMS: JS

package one

class Foo

companion fun Foo.greet(): String = "Hi"

fun greet(): String = "Another"

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionFunctionKt.class[greet]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionFunctionKt.class[greet]
