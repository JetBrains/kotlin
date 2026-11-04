// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

class C {
    @JvmExposeBoxed
    override fun toString(): String = "C"
}

interface Generic<T> {
    fun accept(t: T): String
}

class AcceptsUInt : Generic<UInt> {
    @JvmExposeBoxed
    override fun accept(t: UInt): String = "$t"
}

// LIGHT_ELEMENTS_NO_DECLARATION: AcceptsUInt.class[accept-WZ4Q5Ns]
