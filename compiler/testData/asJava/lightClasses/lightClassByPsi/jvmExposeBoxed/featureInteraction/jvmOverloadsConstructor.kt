// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class IntWrapper(val s: Int)

class Baz {
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed
    @JvmOverloads
    constructor(o: Int = 0, k: IntWrapper = IntWrapper(1)) {

    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[Baz], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]