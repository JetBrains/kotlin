// KT-55828
// IGNORE_BACKEND_K2: NATIVE

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
