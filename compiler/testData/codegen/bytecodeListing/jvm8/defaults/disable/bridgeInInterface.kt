// JVM_DEFAULT_MODE: disable

// This test is checking that:
// 1) Test2 does not have the bridge (with Any in the signature)
// 2) Test2 has a DefaultImpls class with a static version of the specialized method (with String), but not the bridge
// 3) TestClass has both the specialized method and the bridge

interface Test<T> {
    fun test(p: T): T {
        return null!!
    }
}

interface Test2: Test<String> {
    override fun test(p: String): String {
        return p
    }
}

class TestClass : Test2
