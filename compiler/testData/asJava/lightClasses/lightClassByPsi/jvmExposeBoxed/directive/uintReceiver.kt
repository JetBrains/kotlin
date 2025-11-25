// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

class TopLevelClass {
    fun UInt.foo(): UInt = this
}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelClass.class[foo-IKrLr70]