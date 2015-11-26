operator fun String.inc() : String {
    if (this == "") {
        return "done"
    }
    var s = ""
    return ++s
}

fun box() : String {
    var s = "11test"
    return if (++s == "done") "OK" else "FAIL"
}