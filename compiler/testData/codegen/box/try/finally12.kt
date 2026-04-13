fun box(): String {
    var x: Any = 42
    do {
        try {
            throw Exception()
        } catch(e: Throwable) {
            x = "OK"
            break
            x = 117
        } finally {
            return x.toString()
        }
    } while (false)
}
