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