// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo

companion fun Foo.a(): Int = 1

fun Foo.b(): Int = 2

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionMixedWithRegularExtensionKt.class[a]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionMixedWithRegularExtensionKt.class[a]
