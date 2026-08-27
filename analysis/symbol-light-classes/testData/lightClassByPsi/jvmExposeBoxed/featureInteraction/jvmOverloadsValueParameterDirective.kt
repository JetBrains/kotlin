// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

// The implicit mode annotates nothing, so a 'JvmOverloads' wrapper has no 'JvmExposeBoxed' to inherit. Once an overload has
// no value class left, it is an ordinary method under its Kotlin name.

@JvmInline
value class StringWrapper(val s: String)

@JvmOverloads
fun topLevel(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

class Baz {
    @JvmOverloads
    fun memberLevel(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""
}

// LIGHT_ELEMENTS_NO_DECLARATION: Baz.class[memberLevel-WwgAR2g], JvmOverloadsValueParameterDirectiveKt.class[topLevel-WwgAR2g], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
