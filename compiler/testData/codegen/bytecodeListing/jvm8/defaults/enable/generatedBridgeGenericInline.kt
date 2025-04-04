// JVM_DEFAULT_MODE: enable
// WITH_STDLIB

interface Test<T> {
    fun test(p: T): T = null!!
}

class TestClass : Test<UInt>
