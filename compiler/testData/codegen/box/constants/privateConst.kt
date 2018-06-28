// IGNORE_BACKEND: JVM_IR
private const val z = "OK";

fun box(): String {
    return {
        z
    }()
}