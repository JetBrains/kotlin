// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val s: Int)

@JvmOverloads
fun topLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)

class Baz {
    @JvmOverloads
    fun memberLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[memberLevel-7j0DjTs;memberLevel-8dC4te0;memberLevel-jDlQq38], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]