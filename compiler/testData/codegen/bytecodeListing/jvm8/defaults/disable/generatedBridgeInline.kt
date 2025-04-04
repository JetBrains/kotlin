// JVM_DEFAULT_MODE: disable
// WITH_STDLIB

// checking that TestClass has correctly generated mangled override
interface Test {
    fun test(p: UInt): UInt = null!!
}

class TestClass : Test
