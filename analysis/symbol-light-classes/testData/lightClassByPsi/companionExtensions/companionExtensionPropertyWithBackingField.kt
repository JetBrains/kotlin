// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo

companion val Foo.size: Int = 1

// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionPropertyWithBackingFieldKt.class[getSize]
