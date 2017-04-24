// WITH_RUNTIME

fun box(): String {
    for (c in "") {
        return "Fail"
    }

    return iterate("""""")
}

fun iterate(s: String): String {
    for (c in s + "") {
        return "Fail"
    }

    return "OK"
}
