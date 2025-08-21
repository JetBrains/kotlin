// JVM_DEFAULT_MODE: enable

// This test is checking that:
// 1) Test2 has both the specialized method (with String in the signature) and the bridge (with Any)
// 2) Test2 has a DefaultImpls class with a static version of the specialized method, but not the bridge
// 3) TestClass has both the specialized method and the bridge (see KT-73954)

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
