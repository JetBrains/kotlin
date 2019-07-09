// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var s = ""
    for (c in StringBuilder("OK")) {
        s += c
    }
    return s
}