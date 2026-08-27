// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// Every 'JvmOverloads' wrapper inherits the annotations of the original declaration. An overload which still has a value
// class is split into a mangled regular method and a boxed one named after 'JvmExposeBoxed', while an overload without one
// stays a single method under the 'JvmName' name, carrying both annotations.

@JvmInline
value class StringWrapper(val s: String)

class Baz {
    @JvmExposeBoxed("exposedName")
    @JvmName("jvmName")
    @JvmOverloads
    fun foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[exposedName], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
