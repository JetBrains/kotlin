fun box(): String {
    try {
        null!!
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
