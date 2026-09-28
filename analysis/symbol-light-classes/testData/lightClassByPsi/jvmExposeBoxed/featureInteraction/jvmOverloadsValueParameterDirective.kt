// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

// In implicit mode, the source declaration has no explicit `@JvmExposeBoxed` annotation for `@JvmOverloads` to copy. Once a
// generated overload omits the value-class parameter, it needs no boxed wrapper and is emitted as an ordinary method with
// its Kotlin name.

@JvmInline
value class StringWrapper(val s: String)

@JvmOverloads
fun topLevel(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

class Baz {
    @JvmOverloads
    fun memberLevel(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[memberLevel-WwgAR2g], JvmOverloadsValueParameterDirectiveKt.class[topLevel-WwgAR2g], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
