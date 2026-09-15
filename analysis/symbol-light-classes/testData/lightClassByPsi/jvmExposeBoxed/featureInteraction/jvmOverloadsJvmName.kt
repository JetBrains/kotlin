// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// `@JvmOverloads` copies the original declaration's annotations to each generated overload. A signature that still contains
// the value-class parameter produces a regular method named by `@JvmName` and a boxed wrapper named by `@JvmExposeBoxed`.
// After that parameter is omitted, the backend emits only the `@JvmName` method and retains both annotations on it.

@JvmInline
value class StringWrapper(val s: String)

class Baz {
    @JvmExposeBoxed("exposedName")
    @JvmName("jvmName")
    @JvmOverloads
    fun foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[exposedName], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
