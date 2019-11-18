// IGNORE_BACKEND_FIR: JVM_IR
private const val z = "OK";

fun box(): String {
    return {
        z
    }()
}