// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class Explicit(val a: UInt) {
    @JvmExposeBoxed
    constructor(param1: UInt, param2: String) : this(param1)
}

@JvmExposeBoxed
@JvmInline
value class Container(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1)

    private constructor(param1: UInt, param2: Int) : this(param1)
}

@JvmInline
value class NonExposed(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1)
}

// All parameters of the primary constructor have default values, so it produces the exposed no-arg constructor
@JvmExposeBoxed
@JvmInline
value class PrimaryClaims(val a: UInt = 1u) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// Only the primary constructor can produce the exposed no-arg constructor
@JvmExposeBoxed
@JvmInline
value class SecondaryClaims(val a: UInt) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Container.class[constructor-impl;constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], Explicit.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], NonExposed.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], PrimaryClaims.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], SecondaryClaims.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl]
