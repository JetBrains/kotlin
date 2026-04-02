// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class Z(val value: Any = {})

// LIGHT_ELEMENTS_NO_DECLARATION: Z.class[constructor-impl;constructor_impl$lambda$0;equals-impl;equals-impl0;hashCode-impl;toString-impl]