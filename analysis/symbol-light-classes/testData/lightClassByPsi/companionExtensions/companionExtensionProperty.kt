// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo

companion val Foo.size: Int
    get() = 1

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionPropertyKt.class[size]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionPropertyKt.class[getSize]
