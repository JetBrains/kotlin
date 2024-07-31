// TARGET_BACKEND: JVM
// KT-37592

class A {
    fun test1(): Boolean {
        val foo: String.() -> Boolean = {false} // (1)
        fun String.foo(): Boolean {return true} // (2)
        return "1".foo() // resolves to (2)
    }
    fun test2(): Boolean {
        val foo: String.() -> Boolean = {false} // (1)
        fun String.foo(): Boolean {return true} // (2)
        with("2") {
            return foo() // resolves to (1)
        }
    }
}

class B {
    val foo: String.() -> Boolean = {false} // (1)
    fun String.foo(): Boolean {return true} // (2)

    fun test3(): Boolean {
        return "1".foo() // resolves to (2)
    }
    fun test4(): Boolean {
        with("2") {
            return foo() // resolves to (2)
        }
    }
}

fun box(): String {
    if (A().test1() == false) {
        return "Fail"
    }

    if (A().test2() == true) {
        return "Fail" // Bug KT-70310
    }

    if (B().test3() == false) {
        return "Fail"
    }

    if (B().test4() == false) {
        return "Fail"
    }

    return "OK"
}