// B
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: disable

interface A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun hidden() {}

    @JvmSynthetic
    fun synthetic() {}

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    val hiddenProperty: Int
        get() = 1

    val hiddenGetter: Int
        @Deprecated("", level = DeprecationLevel.HIDDEN) get() = 1

    val syntheticGetter: Int
        @JvmSynthetic get() = 1
}

// The bridges in `DefaultImpls` of `B` are synthetic
interface B : A
