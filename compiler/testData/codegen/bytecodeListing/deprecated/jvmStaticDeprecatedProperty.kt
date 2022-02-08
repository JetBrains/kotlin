// WITH_STDLIB
// JVM_TARGET: 1.8

class TestClass {
    companion object {
        @Deprecated("")
        @JvmStatic
        val a: Int  = 1
    }
}

object TestObject {
    @Deprecated("")
    @JvmStatic
    val a: Int  = 1
}

interface TestInterface {
    companion object {
        @Deprecated("")
        @JvmStatic
        val a: Int  = 1
    }
}