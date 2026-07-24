// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

class OriginalClass

@JvmInline
value class AnotherValueClass(val original: OriginalClass)

@JvmInline
value class ValueClass(val another: AnotherValueClass)

// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[constructor-impl;equals-impl;equals-impl0;getAnother--_GsEoE;hashCode-impl;toString-impl]
