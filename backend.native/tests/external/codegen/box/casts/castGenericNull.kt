fun <T> castToString(t: T) {
    t as String
}


fun box(): String {
    try {
        castToString<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
