// JVM_DEFAULT_MODE: disable
// LANGUAGE: +JvmEnhancedBridges
// WITH_STDLIB

interface I1 {
    fun foo(): Any
}

interface I2 : I1 {
    override fun foo(): UInt
    // foo(): Object bridge to the mangled foo-pVg5ArA() method
}

interface I3<T: UInt?>: I2 {
    override fun foo(): T & Any
    // Object bridge from I1 and the mangled int bridge from I2
}

interface I4 : I2 {
    override fun foo(): UInt
    // Only Object bridge from I1
}
