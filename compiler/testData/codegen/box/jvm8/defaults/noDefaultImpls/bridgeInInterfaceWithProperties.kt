// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test<T> {
    var test: T
        get() = null!!
        set(value) {
            null!!
        }
}
var result = "fail"

interface Test2 : Test<String> {
    override var test: String
        get() = result
        set(value) {
            result = value
        }
}

class TestClass : Test2

fun <T> execute(t: Test<T>, p: T): T {
    t.test = p
    return t.test
}

fun box(): String {
    return execute(TestClass(), "OK")
}
