// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class IntWrapper @JvmExposeBoxed constructor(val i: Int = 0)

class RegularClassWithValueConstructor(val property: IntWrapper = IntWrapper(1))

@OptIn(ExperimentalStdlibApi::class)
class RegularClassWithValueConstructorAndAnnotation @JvmExposeBoxed constructor(val property: IntWrapper = IntWrapper(2))

// The default value of `c` references another parameter
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class RegularClassWithDependentDefault(val property: IntWrapper = IntWrapper(3), b: Boolean = true, c: Boolean = b)

// LIGHT_ELEMENTS_NO_DECLARATION: IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], RegularClassWithDependentDefault.class[getProperty-7j0DjTs], RegularClassWithValueConstructor.class[getProperty-7j0DjTs], RegularClassWithValueConstructorAndAnnotation.class[getProperty-7j0DjTs]