// IGNORE_BACKEND_FIR: JVM_IR
enum class Empty

fun box(): String {
    if (Empty.values().size != 0) return "Fail: ${Empty.values()}"

    try {
        val found = Empty.valueOf("nonExistentEntry")
        return "Fail: $found"
    }
    catch (e: Exception) {
        return "OK"
    }
}
