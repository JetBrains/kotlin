// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

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
    @JvmExposeBoxed("explicitBoxedName")
    @JvmName("ignoredJvmName")
    internal fun explicitlyNamedFun(value: ValueInt): ValueInt = value

    @JvmExposeBoxed
    @JvmName("jvmNamed")
    internal fun jvmNamedFun(value: ValueInt): ValueInt = value
}

// LIGHT_ELEMENTS_NO_DECLARATION: InternalNames.class[explicitBoxedName], ValueInt.class[constructor-impl;equals-impl;equals-impl0;getInternalProperty-impl$main;getPublishedApiProperty-impl;hashCode-impl;internalFun-impl$main;publishedApiFun-impl;toString-impl]
