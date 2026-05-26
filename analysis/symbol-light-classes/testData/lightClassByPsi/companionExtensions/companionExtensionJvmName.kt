// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

class Foo

@JvmName("renamedExt")
companion fun Foo.original(): Int = 1

@get:JvmName("getRenamedProperty")
companion val Foo.property: Int
    get() = 2

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionJvmNameKt.class[original;property]
// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionJvmNameKt.class[getRenamedProperty;renamedExt]
