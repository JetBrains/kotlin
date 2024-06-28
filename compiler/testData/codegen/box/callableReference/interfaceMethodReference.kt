// KT-66471

interface TestInterface {
    fun foo()
}

var result: Any? = "Fail: not initialized"

object TestObject {
    private val test = run {
        result = TestInterface::foo
        "OK"
    }

    fun bar() {}
}

fun box(): String {
    TestObject.bar()
    return if (result is Function<*>) "OK" else result.toString()
}