// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

abstract class A {
    abstract fun abstractMethod(a: UInt): String
}

class B() : A() {
    @JvmExposeBoxed
    override fun abstractMethod(a: UInt): String {
        if (a == 1u) return "OK"
        else return "FAIL $a"
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: A.class[abstractMethod]
// LIGHT_ELEMENTS_NO_DECLARATION: A.class[abstractMethod-WZ4Q5Ns], B.class[abstractMethod-WZ4Q5Ns]