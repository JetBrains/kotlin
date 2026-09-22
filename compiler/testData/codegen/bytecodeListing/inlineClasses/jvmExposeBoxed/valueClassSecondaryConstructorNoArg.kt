// WITH_STDLIB

// KT-89068: only the primary constructor can produce the exposed no-arg constructor.

@file:OptIn(ExperimentalStdlibApi::class)

// All parameters of the primary constructor have default values, so the no-arg constructor is generated.
@JvmExposeBoxed
@JvmInline
value class PrimaryClaims(val a: UInt = 1u) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// The primary constructor has no default value, so no no-arg constructor is generated.
@JvmExposeBoxed
@JvmInline
value class SecondaryClaims(val a: UInt) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}
