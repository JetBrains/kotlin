fun box(): String {
    val obj = "" as java.lang.Object

    val e = IllegalArgumentException()
    try {
        synchronized (obj) {
            throw e
        }
    }
    catch (caught: Throwable) {
        if (!(caught identityEquals e)) return "Fail: $caught"
        // If monitorexit didn't happen (a finally block failed), this assertion would fail
        assertThatThreadDoesNotOwnMonitor(obj)
    }

    return "OK"
}

fun assertThatThreadDoesNotOwnMonitor(obj: java.lang.Object) {
    try {
        obj.wait(1)
        throw IllegalStateException("Not owning a monitor!")
    }
    catch (e: IllegalMonitorStateException) {
        // OK
    }
}