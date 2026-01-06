// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class TopLevelClass {
    fun UInt.foo(): UInt = this
}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelClass.class[foo-IKrLr70]