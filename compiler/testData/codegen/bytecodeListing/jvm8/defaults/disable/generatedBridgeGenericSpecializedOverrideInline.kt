// JVM_DEFAULT_MODE: disable
// WITH_STDLIB
// ENABLE_INTERFACE_BRIDGES

interface Root<T> {
    fun test(p: T): T = null!!
}

interface Specialized : Root<UInt> {
    override fun test(p: UInt): UInt = 2u
}


class TestClass : Specialized
