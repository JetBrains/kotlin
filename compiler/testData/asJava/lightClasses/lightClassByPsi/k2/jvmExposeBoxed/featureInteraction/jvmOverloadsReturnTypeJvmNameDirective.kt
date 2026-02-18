// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val s: Int)

@JvmOverloads
@JvmName("jvmTopLevel")
fun topLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)

class Baz {
    @JvmOverloads
    @JvmName("jvmMemberLevel")
    fun memberLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)
}

// LIGHT_ELEMENTS_NO_DECLARATION: IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]