// WITH_STDLIB
// TARGET_PLATFORM: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class ValueInt(val i: Int) {
    internal fun internalFun() {}

    internal val internalProperty: Int
        get() = i

    @PublishedApi
    internal fun publishedApiFun() {}

    @PublishedApi
    internal val publishedApiProperty: Int
        get() = i
}

@OptIn(ExperimentalStdlibApi::class)
class InternalNames {
    @JvmExposeBoxed
    internal fun unnamedFun(value: ValueInt): ValueInt = value

    @JvmExposeBoxed("explicitBoxedName")
    @JvmName("ignoredJvmName")
    internal fun explicitlyNamedFun(value: ValueInt): ValueInt = value

    @JvmExposeBoxed
    @JvmName("jvmNamed")
    internal fun jvmNamedFun(value: ValueInt): ValueInt = value
}
