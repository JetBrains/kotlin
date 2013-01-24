fun box(): String {
    try {
        if ((null : Int?)!! == 10) return "Fail #1"
        return "Fail #2"
    }
    catch (e: Exception) {
        return "OK"
    }
}
