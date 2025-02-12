// JVM_DEFAULT_MODE: no-compatibility

// This test is checking that:
// 1) Test2 has both the specialized method (with String in the signature) and the bridge (with Any)
// 2) Test2 does not have a DefaultImpls class
// 3) TestClass has neither the specialized method, nor the bridge

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
