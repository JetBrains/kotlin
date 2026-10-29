// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class Implicit(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1)

    private constructor(param1: UInt, param2: Int) : this(param1)
}

// All parameters of the primary constructor have default values, so it produces the exposed no-arg constructor
@JvmInline
value class PrimaryClaims(val a: UInt = 1u) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// Only the primary constructor can produce the exposed no-arg constructor
@JvmInline
value class SecondaryClaims(val a: UInt) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Implicit.class[constructor-impl;constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], PrimaryClaims.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl], SecondaryClaims.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl]
