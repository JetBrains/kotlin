// See: https://youtrack.jetbrains.com/issue/KT-45319
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JS

fun breakInDoWhileCondition(): String {
    var i = 0
    while (true) {
        ++i
        var j = 0
        do {
            ++j
        } while (break)
        if (j != 1) return "FAIL1"
        if (i == 3) break
    }
    if (i != 3) return "FAIL2"
    return "OK"
}

fun breakInWhileCondition(): String {
    var i = 0
    while (true) {
        ++i
        var j = 0
        while (break) {
            j++
        }
        return "FAIL3"
    }
    if (i != 1) return "FAIL4"
    return "OK"
}

fun box(): String {
    val breakInDoWhileResult = breakInDoWhileCondition()
    if (breakInDoWhileResult != "OK") return breakInDoWhileResult
    return breakInWhileCondition()
}
