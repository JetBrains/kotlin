// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    class A {
        var result: String = "Fail";
        init {
            result = "OK"
        }
    }

    return (::A)().result
}
