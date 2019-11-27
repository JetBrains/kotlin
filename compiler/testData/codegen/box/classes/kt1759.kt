// IGNORE_BACKEND_FIR: JVM_IR
class Greeter(var name : String) {
    fun greet() {
        name = name.plus("")
    }
}

fun box() : String {
    Greeter("OK").greet()
    return "OK"
}
