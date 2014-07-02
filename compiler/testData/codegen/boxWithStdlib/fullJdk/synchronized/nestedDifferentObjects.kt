fun box(): String {
    val obj = "" as java.lang.Object
    val obj2 = "1" as java.lang.Object

    synchronized (obj) {
        synchronized (obj2) {
            obj.wait(1)
            obj2.wait(1)
        }
    }

    return "OK"
}