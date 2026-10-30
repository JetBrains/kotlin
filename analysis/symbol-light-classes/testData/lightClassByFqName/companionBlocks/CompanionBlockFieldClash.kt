// one.Regular
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

class Regular(val x: Int) {
    val y: Int = 1
    val z: Int = 2
    @JvmField val w: Int = 3
    val noField: Int = 4

    companion {
        val x: String = "x"
        @JvmField val y: String = "y"
        const val z: String = "z"

        // A public instance field keeps its name, so the static one is renamed
        val w: String = "w"

        // No static field, so no clash
        val noField: String get() = "noField"
    }
}
