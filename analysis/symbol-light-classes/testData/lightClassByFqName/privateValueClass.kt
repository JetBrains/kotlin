// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

@JvmInline
private value class ValueClass(val value: String)

// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
