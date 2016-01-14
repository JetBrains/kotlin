fun box(): String {
    val obj = "" as java.lang.Object
    val e = IllegalArgumentException()
    fun m(): Nothing = throw e
    try {
        synchronized (m()) {
            throw AssertionError("Should not have reached this point")
        }
    }
    catch (caught: Throwable) {
        if (!(caught.identityEquals(e))) return "Fail: $caught"
    }

    return "OK"
}