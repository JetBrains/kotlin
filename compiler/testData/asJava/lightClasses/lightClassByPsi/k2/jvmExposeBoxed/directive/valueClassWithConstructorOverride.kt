// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

interface Interface {
    val value: Int
}

@JvmInline
value class ValueClass(override val value: Int) : Interface
// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]