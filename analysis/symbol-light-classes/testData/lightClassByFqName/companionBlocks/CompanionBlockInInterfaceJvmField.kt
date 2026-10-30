// one.WithJvmField
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

interface WithJvmField {
    companion {
        @JvmField val STRING: String = "s"
        @JvmField val nullable: Int? = null
    }
}
