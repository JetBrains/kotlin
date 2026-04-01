// JVM_DEFAULT_MODE: disable
// WITH_STDLIB
// LANGUAGE: +JvmEnhancedBridges

interface Root<T> {
    fun test(p: T): T = null!!
}

interface Specialized : Root<UInt> {
    override fun test(p: UInt): UInt = 2u
}


class TestClass : Specialized
