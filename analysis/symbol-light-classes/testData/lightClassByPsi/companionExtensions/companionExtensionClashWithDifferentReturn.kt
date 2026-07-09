// LANGUAGE: +CompanionBlocks +CompanionExtensions
package one

class Foo

companion fun Foo.greet(): String = "Hi"

fun greet(): Int = 0

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionClashWithDifferentReturnKt.class[greet]
