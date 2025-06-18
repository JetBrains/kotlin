// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class IntWrapper @JvmExposeBoxed constructor(val i: Int = 0)

class RegularClassWithValueConstructor(val property: IntWrapper = IntWrapper(1))

@OptIn(ExperimentalStdlibApi::class)
class RegularClassWithValueConstructorAndAnnotation @JvmExposeBoxed constructor(val property: IntWrapper = IntWrapper(2))
