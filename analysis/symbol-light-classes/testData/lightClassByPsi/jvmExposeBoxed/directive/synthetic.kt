// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class StringWrapper(val s: String) {
    @JvmSynthetic
    fun ok(): String = s
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: StringWrapper.class[ok]
// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]