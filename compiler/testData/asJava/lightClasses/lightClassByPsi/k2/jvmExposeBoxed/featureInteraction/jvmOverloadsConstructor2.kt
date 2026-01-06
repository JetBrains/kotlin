// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class IntWrapper(val s: Int)

class Baz {
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed
    @JvmOverloads
    constructor(o: IntWrapper = IntWrapper(0), k: Int = 1) {

    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]