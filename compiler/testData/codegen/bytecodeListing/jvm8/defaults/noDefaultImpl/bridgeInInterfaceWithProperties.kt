// JVM_DEFAULT_MODE: all

// This test is checking that:
// 1) Test2 has both the specialized methods (with String in the signature) and the bridges (with Any) for both property accessors
// 2) Test2 does not have a DefaultImpls class
// 3) TestClass has neither the specialized methods, nor the bridges

interface Test<T> {
    var T.test: T
        get() = null!!
        set(value) {
            null!!
        }
}

interface Test2 : Test<String> {
    override var String.test: String
        get() = ""
        set(value) {}
}

class TestClass : Test2
