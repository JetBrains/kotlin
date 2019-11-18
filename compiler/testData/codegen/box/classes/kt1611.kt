// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    return Foo().doBar("OK")
}

class Foo() {
    val bar : (str : String) -> String = { it }

    fun doBar(str : String): String {
        return bar(str);
    }
}
