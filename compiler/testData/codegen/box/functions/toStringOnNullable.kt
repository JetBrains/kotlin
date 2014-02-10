fun nullAny(): Any? = null

fun box(): String {
    if (null.toString() != "null") return "Fail 1"
    if (nullAny().toString() != "null") return "Fail 2"
    if ("${null}" != "null") return "Fail 3"

    return "OK"
}
