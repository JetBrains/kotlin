fun box(): String {
    var obj = "0" as java.lang.Object
    val result = synchronized (obj) {
        "239"
    }

    if (result != "239") return "Fail: $result"

    return "OK"
}