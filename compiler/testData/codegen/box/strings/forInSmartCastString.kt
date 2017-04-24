// WITH_RUNTIME

fun iterate(x: Any?): Any? {
    if (x is String) {
        for (i in x) {
            return "OK"
        }
    }

    return "Fail"
}

fun box(): String {
    val string = "f"
    return iterate(string) as String
}
