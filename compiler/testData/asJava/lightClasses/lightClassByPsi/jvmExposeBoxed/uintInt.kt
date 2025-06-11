// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class TopLevelClass {
    fun UInt.foo(i: Int): UInt = this + i.toUInt()
}
