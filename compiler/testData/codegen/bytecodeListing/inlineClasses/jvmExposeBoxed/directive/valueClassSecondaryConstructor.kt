// WITH_STDLIB
// JVM_EXPOSE_BOXED

// KT-89068: Expose secondary constructor of a value class

@JvmInline
value class Implicit(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1)
}
