// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

class TopLevelClass {
    fun UInt.foo(i: Int): UInt = this + i.toUInt()
}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelClass.class[foo-mPSJhXU]