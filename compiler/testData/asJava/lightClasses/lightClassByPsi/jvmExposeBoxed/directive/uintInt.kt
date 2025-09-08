// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

class TopLevelClass {
    fun UInt.foo(i: Int): UInt = this + i.toUInt()
}
