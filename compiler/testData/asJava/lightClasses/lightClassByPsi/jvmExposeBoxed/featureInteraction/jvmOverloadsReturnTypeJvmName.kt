// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class IntWrapper(val s: Int)

@JvmOverloads
@JvmName("jvmTopLevel")
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("boxedTopLevel")
fun topLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)

class Baz {
    @JvmOverloads
    @JvmName("jvmMemberLevel")
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed("boxedMemberLevel")
    fun memberLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[boxedMemberLevel;boxedMemberLevel;boxedMemberLevel], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], JvmOverloadsReturnTypeJvmNameKt.class[boxedTopLevel;boxedTopLevel;boxedTopLevel]