// WITH_STDLIB

// KT-89068: the no-arg constructor is generated at most once per class.

@file:OptIn(ExperimentalStdlibApi::class)

// The primary constructor claims the no-arg constructor, so the secondary must not generate a second one.
@JvmExposeBoxed
@JvmInline
value class PrimaryClaims(val a: UInt = 1u) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

// The primary has no default value, so the secondary is free to generate the no-arg constructor.
@JvmExposeBoxed
@JvmInline
value class SecondaryClaims(val a: UInt) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}
