class A {
    fun test(x: String? = "x", a: String?, b: String?, y: String? = "y") {

    }

    fun box(): String {
        test(b = "K", a = "O")
        return "OK"
    }
}
// Test argument reordering when call site argument order differs from declaration one
// 18 LOAD
// 5 STORE