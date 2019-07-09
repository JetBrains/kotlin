// IGNORE_BACKEND: JVM_IR
class A {
    fun test(x: String?, a: String?, b: String?){}

    fun box(): String {
        test(x = "x", b = "K", a = "O")
        return "OK"
    }
}
// Test argument reordering when call site argument order differs from declaration one
// 4 LOAD
// 2 STORE