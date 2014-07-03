fun box(): String {
    var obj0 = "0" as java.lang.Object
    var obj1 = "1" as java.lang.Object

    var v = obj0
    synchronized (v) {
        v = obj1
    }
    assertThatThreadDoesNotOwnMonitor(obj0)

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