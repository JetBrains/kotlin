// one.Regular
// LANGUAGE: +CompanionBlocks
// LIBRARY_PLATFORMS: JVM
package one

open class Regular {
    lateinit var publicLateinit: String
    protected lateinit var protectedLateinit: String
    lateinit var internalSetter: String
        internal set
    @JvmField internal val internalJvmField: String = "4"
    lateinit var internalStaticJvmField: String

    companion {
        // A public instance field keeps its name, so the static one is renamed
        val publicLateinit: Int = 0
        val protectedLateinit: Int = 0

        // An internal field isn't public API, so the instance one is renamed
        val internalSetter: Int = 0
        val internalJvmField: Int = 0

        // An internal static field is renamed next to a public instance one, even if it's a `@JvmField`
        @JvmField internal val internalStaticJvmField: Int = 0
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Regular.class[setInternalSetter$main]
