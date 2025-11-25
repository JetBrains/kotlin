// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class Z(val value: Any = {})

// LIGHT_ELEMENTS_NO_DECLARATION: Z.class[Z;constructor-impl;constructor_impl$lambda$0;equals-impl;equals-impl0;hashCode-impl;toString-impl]