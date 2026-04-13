// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class TopLevelClass {
    var topLevelClassProperty: UInt = 1u
}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelClass.class[getTopLevelClassProperty-pVg5ArA;setTopLevelClassProperty-WZ4Q5Ns]