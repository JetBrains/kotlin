// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class C(val value: Int) {
    fun member(): Int = value

    companion {
        fun fromInt(value: Int): C = C(value)
    }
}
