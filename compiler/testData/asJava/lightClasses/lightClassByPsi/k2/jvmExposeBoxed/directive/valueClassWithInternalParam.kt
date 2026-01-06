// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

class OriginalClass

@JvmInline
value class ValueClass(internal val value: OriginalClass)
// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]