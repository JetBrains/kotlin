// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val x = "OK"
    class Aaa {
        val y = x
    }

    return Aaa().y
}
