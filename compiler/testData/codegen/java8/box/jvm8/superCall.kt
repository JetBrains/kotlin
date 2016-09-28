// JVM_TARGET: 1.8


interface Test {
    fun test(): String {
        return "OK"
    }
}

interface Test2 : Test {
    override fun test(): String {
        return super.test()
    }
}


class TestClass : Test2 {

}


fun box(): String {
    return TestClass().test()
}