// JVM_TARGET: 17
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class IC(val x: Int)

@JvmRecord
data class Rec(val u: UInt, val i: Int, val ic: IC) {
    val body: UInt get() = 1u
    val Int.ext: UInt get() = 2u
    val UInt.receiverExt: Int get() = 3
    var mutable: UInt
        get() = 4u
        set(value) {}

    companion object {
        @JvmStatic
        val c: UInt get() = 1u
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Rec.class[c;component1;component3;copy]
// LIGHT_ELEMENTS_NO_DECLARATION: IC.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Rec.class[component1-pVg5ArA;component3-fhib4bs;copy-xEyznbc;getC-pVg5ArA;getC-pVg5ArA;mutable]
