// IGNORE_BACKEND: JVM_IR
fun box(): String {
    if (false) {
        try {
            null!!
        } catch (e: Exception) {
            throw e
        }
    }
    return "OK"
}