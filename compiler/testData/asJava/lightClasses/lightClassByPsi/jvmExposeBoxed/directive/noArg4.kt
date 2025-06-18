// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper constructor(val i: Int = 0)

class RegularClassWithValueConstructor(val property: IntWrapper = IntWrapper(1))

class RegularClassWithValueConstructorAndAnnotation constructor(val property: IntWrapper = IntWrapper(2))
