// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper constructor(val i: Int = 0)

class RegularClassWithValueConstructor(val property: IntWrapper = IntWrapper(1))

class RegularClassWithValueConstructorAndAnnotation constructor(val property: IntWrapper = IntWrapper(2))

// LIGHT_ELEMENTS_NO_DECLARATION: IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], RegularClassWithValueConstructor.class[getProperty-7j0DjTs], RegularClassWithValueConstructorAndAnnotation.class[getProperty-7j0DjTs]