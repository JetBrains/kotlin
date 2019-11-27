// IGNORE_BACKEND_FIR: JVM_IR
class Data

fun newInit(f: Data.() -> Data) = Data().f()

class TestClass {
    val test: Data = newInit()  { this }
}

fun box() : String {
    TestClass()
    return "OK"
}