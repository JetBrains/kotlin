// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

class Baz {
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed
    @JvmOverloads
    fun foo(o: String = "O", k: String = "K"): StringWrapper = StringWrapper(o + k)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[foo-3h5-OkU;foo-AAZA6W0;foo-K4fyztM], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]