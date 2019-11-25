// IGNORE_BACKEND_FIR: JVM_IR
fun test1(str: String): String {
    data class A(val x: Int) {
        fun foo() = str
    }
    return A(0).copy().foo()
}

class TestClass(val x: String) {
    fun foo(): String {
        data class A(val x: Int) {
            fun foo() = this@TestClass.x
        }
        return A(0).copy().foo()
    }
}

fun test2(str: String): String = TestClass(str).foo()

fun test3(str: String): String {
    var xx = ""
    data class A(val x: Int) {
        fun foo(): String { xx = str; return xx }
    }
    return A(0).copy().foo()
}

fun test4(str: String): String {
    var xx = ""
    fun bar(s: String): String { xx = s; return xx }
    data class A(val x: Int) {
        fun foo(): String = bar(str)
    }
    return A(0).copy().foo()
}

fun box(): String {
    return when {
        test1("test1") != "test1" -> "Failed #1 (parameter capture)"
        test2("test2") != "test2" -> "Failed #2 ('this' capture)"
        test3("test3") != "test3" -> "Failed #3 ('var' capture)"
        test4("test4") != "test4" -> "Failed #4 (local function capture)"
        else -> "OK"
    }
}