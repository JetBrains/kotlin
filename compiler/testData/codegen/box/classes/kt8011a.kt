// IGNORE_BACKEND_FIR: JVM_IR
fun testFun1(str: String): String {
    val capture = str

    class A {
        val x = capture
    }

    return A().x
}


fun testFun2(str: String): String {
    class A {
        val x = str
    }
    fun bar() = A()
    return bar().x
}


class TestClass(val str: String) {
    var xx: String? = null

    init {
        class A {
            val x = str
        }

        xx = A().x
    }
}

fun testFun3(str: String): String = TestClass(str).xx!!


fun String.testFun4(): String {
    class A {
        val x = this@testFun4
    }
    return A().x
}


fun box(): String {
    return when {
        testFun1("test1") != "test1" -> "Fail #1 (local class with capture)"
        testFun2("test2") != "test2" -> "Fail #2 (local class with capture ctor in another context)"
        testFun3("test3") != "test3" -> "Fail #3 (local class with capture ctor in init{ ... })"
        "test4".testFun4() != "test4" -> "Fail #4 (local class with extension receiver)"
        else -> "OK"
    }
}
