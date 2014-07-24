fun box(): String {
    val obj = "" as java.lang.Object
    try {
        obj.wait(1)
        return "Fail: exception should have been thrown"
    }
    catch (e: IllegalMonitorStateException) {
        // OK
    }

    synchronized (obj) {
        obj.wait(1)
    }

    return "OK"
}