// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

open class A {
    open fun openMethod(a: UInt): String {
        if (a == 1u) return "OK"
        else return "FAIL $a"
    }
}

class B() : A() {
    @JvmExposeBoxed
    override fun openMethod(a: UInt): String = super.openMethod(a)
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: A.class[openMethod]
// LIGHT_ELEMENTS_NO_DECLARATION: A.class[openMethod-WZ4Q5Ns], B.class[openMethod-WZ4Q5Ns]