// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// A value class in a return type mangles the name of the regular method, but 'JvmName' brings the two names back together.
// A return type doesn't keep the declarations apart, so only the boxed method is generated.

@JvmInline
value class IntWrapper(val i: Int)

class Exposed {
    @JvmName("renamed")
    @JvmExposeBoxed
    fun boxedReturnType(r: Result<String>?): IntWrapper = IntWrapper(1)

    // A name of its own keeps the boxed method apart from the regular one
    @JvmName("regularName")
    @JvmExposeBoxed("exposedName")
    fun renamedBoxed(r: Result<String>?): IntWrapper = IntWrapper(1)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Exposed.class[exposedName], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
