// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

open class P {
    open val x: UInt get() = 1u

    open var y: UInt = 1u
}

class Q : P() {
    @get:JvmExposeBoxed
    override val x: UInt get() = 2u

    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    override var y: UInt = 2u
}

interface I {
    val z: Int
}

@JvmInline
value class VC(val v: Int) : I {
    override val z: Int get() = v
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: P.class[x]
// LIGHT_ELEMENTS_NO_DECLARATION: P.class[getX-pVg5ArA;getY-pVg5ArA;setY-WZ4Q5Ns], Q.class[getX-pVg5ArA;getY-pVg5ArA;setY-WZ4Q5Ns], VC.class[constructor-impl;equals-impl;equals-impl0;getZ-impl;hashCode-impl;toString-impl]
