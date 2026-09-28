// WITH_STDLIB

// KT-89068: Expose secondary constructor of a value class

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
}

// Not annotated at all - must keep only constructor-impl.
@JvmInline
value class NonExposed(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1)
}
