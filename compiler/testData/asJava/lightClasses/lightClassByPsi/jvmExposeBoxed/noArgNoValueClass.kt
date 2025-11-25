// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class RegularClassWithValueConstructor(val property: Int = 0)

// LIGHT_ELEMENTS_NO_DECLARATION: RegularClassWithValueConstructor.class[RegularClassWithValueConstructor]