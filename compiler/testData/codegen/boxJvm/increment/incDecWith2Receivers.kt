// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID

class MatchSticks {
    operator fun String.inc() = this + "|"
}

fun box(): String {
    var s = ""
    with(MatchSticks()) {
        s++
        s++
    }

    return if (s == "||") {
        "OK"
    } else {
        "NOK: $s"
    }
}
