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

@JvmInline
internal value class InternalValue(val value: Long)

@OptIn(ExperimentalStdlibApi::class)
internal class InternalConstructor @JvmExposeBoxed constructor(
    value: InternalValue,
    callback: () -> Unit = {},
)

// LIGHT_ELEMENTS_NO_DECLARATION: InternalConstructor.class[_init_$lambda$0], InternalNames.class[explicitBoxedName], InternalValue.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], ValueInt.class[constructor-impl;equals-impl;equals-impl0;getInternalProperty-impl$main;getPublishedApiProperty-impl;hashCode-impl;internalFun-impl$main;publishedApiFun-impl;toString-impl]
