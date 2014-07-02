fun box(): String {
    val obj = "" as java.lang.Object

    try {
        synchronized (obj) {
            throw Throwable()
        }
    }
    catch (e: Throwable) {
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