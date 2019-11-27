// IGNORE_BACKEND_FIR: JVM_IR
fun simple() = fun (): Boolean { return true }

fun withLabel() = l@ fun (): Boolean { return@l true }

fun box(): String {
    if (!simple()()) return "Test simple failed"
    if (!withLabel()()) return "Test withLabel failed"

    return "OK"
}