// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

class C {
    companion {
        @JvmName("renamed")
        fun original(): Int = 1

        @get:JvmName("getRenamedProperty")
        val property: Int = 2
    }
}
