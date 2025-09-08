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
