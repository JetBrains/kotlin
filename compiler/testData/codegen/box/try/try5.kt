fun box(): String {
    var x: Any = 42

    try {
        try {
            x = "OK"
            throw Error()
        } catch (e: Exception) {
        }
    } catch (e: Throwable) {
        return x.toString()
    }

    return "fail"
}
