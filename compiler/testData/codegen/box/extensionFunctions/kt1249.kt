// IGNORE_BACKEND_FIR: JVM_IR
//KT-1249 IllegalStateException invoking function property
class TestClass(val body : () -> Unit) : Any() {
    fun run() {
        body()
    }
}

fun box() : String {
    TestClass({}).run()
    return "OK"
}

