// LANGUAGE: +CompanionBlocksAndExtensions
package one

class Foo

typealias FooAlias = Foo

companion fun FooAlias.greet(): String = "Hi"

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionTypeAliasReceiverKt.class[FooAlias;greet]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionTypeAliasReceiverKt.class[greet]
