// JVM_DEFAULT_MODE: enable

// This test is checking that:
// 1) Test2 has both the specialized methods (with String in the signature) and the bridges (with Any) for both property accessors
// 2) Test2 has a DefaultImpls class with static versions of the specialized methods, but not the bridges
// 3) TestClass has both the specialized methods and the bridges (see KT-73954)

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
