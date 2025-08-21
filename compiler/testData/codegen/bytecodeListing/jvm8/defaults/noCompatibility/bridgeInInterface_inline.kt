// JVM_DEFAULT_MODE: no-compatibility
// WITH_STDLIB

interface Test<T> {
    fun test(p: T): T = null!!
}

interface Test2 : Test<UInt> {
    override fun test(p: UInt): UInt = 2u
}


class TestClass : Test2
