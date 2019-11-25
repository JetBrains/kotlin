// IGNORE_BACKEND_FIR: JVM_IR
class Clazz {
    companion object {
        val a = object {
            fun run(x: String) = x
        }
    }
}

fun box() = "OK"
