// B
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +CompanionBlocks
// JVM_DEFAULT_MODE: disable

@JvmInline
value class Value(val x: Int)

interface Base {
    fun overridden()
}

interface A : Base {
    fun String.extension() {}

    val String.extensionProperty: Int
        get() = 1

    var mutable: Int
        get() = 1
        set(value) {}

    fun withDefaultArgument(x: Int = 1) {}

    fun <T> generic(t: T): T = t

    suspend fun suspending() {}

    fun withValueClass(value: Value) {}

    override fun overridden() {}

    @Deprecated("")
    fun deprecated() {}

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun hidden() {}

    @JvmSynthetic
    fun synthetic() {}

    var hiddenGetter: Int
        @Deprecated("", level = DeprecationLevel.HIDDEN) get() = 1
        set(value) {}

    var syntheticGetter: Int
        @JvmSynthetic get() = 1
        set(value) {}

    companion {
        fun companionBlockMember() {}
    }
}

interface B : A
