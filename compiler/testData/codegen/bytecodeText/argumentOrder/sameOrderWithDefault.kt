// IGNORE_BACKEND: JVM_IR
class A {
    fun test(x: String? = "x", a: String?, b: String?, y: String? = "y") {
    }

    fun box(): String {
        test(a = "O", b = "K")
        return "OK"
    }
}

// Test there is no argument reordering when call site argument order same as declaration one
// 9 LOAD
// 2 STORE
