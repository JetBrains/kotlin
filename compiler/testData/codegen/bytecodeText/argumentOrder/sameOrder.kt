class A {
    fun test(x: String?, a: String?, b: String?) {
    }

    fun box(): String {
        test(x = "x", a = "O", b = "K")
        return "OK"
    }
}
// Test there is no argument reordering when call site argument order same as declaration one
// 2 LOAD
// 0 STORE