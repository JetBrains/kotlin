// JVM_DEFAULT_MODE: disable

// This test is checking that:
// 1) Test2 does not have the bridges (with Any in the signature), for both property accessors
// 2) Test2 has a DefaultImpls class with static versions of the specialized methods (with String), but not the bridges
// 3) TestClass has both the specialized methods and the bridges

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
