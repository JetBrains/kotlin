// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class IntWrapper @JvmExposeBoxed constructor(val i: Int = 0)

class RegularClassWithValueConstructor(val property: IntWrapper = IntWrapper(1))

@OptIn(ExperimentalStdlibApi::class)
class RegularClassWithValueConstructorAndAnnotation @JvmExposeBoxed constructor(val property: IntWrapper = IntWrapper(2))

// LIGHT_ELEMENTS_NO_DECLARATION: IntWrapper.class[IntWrapper;constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], RegularClassWithValueConstructor.class[RegularClassWithValueConstructor;getProperty-7j0DjTs], RegularClassWithValueConstructorAndAnnotation.class[RegularClassWithValueConstructorAndAnnotation;getProperty-7j0DjTs]