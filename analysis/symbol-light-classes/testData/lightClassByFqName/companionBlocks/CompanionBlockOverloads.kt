// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

class C {
    companion {
        @JvmOverloads
        fun describe(prefix: String = "x", suffix: String = "y"): String = "$prefix-$suffix"
    }
}
