// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

class C {
    companion {
        const val CONST_VALUE: Int = 42
        @JvmField val jvmFieldValue: String = "field"
        val plainValue: String = "plain"
    }
}
