// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    for (i in seq) {
        if (i == 2) {
            break
            return "failed: break didn't skip the current iteration"
        }
        if (i == 3) return "failed: break didn't skip the next iteration"
    }
    var shouldBeFalse = true
    for (i in seq) {
        if (i == 2) {
            continue
            return "failed: continue didn't skip the current iteration"
        }
        if (i == 3) shouldBeFalse = false
    }
    if (shouldBeFalse) return "failed: continue skipped more than one iteration"
    return "OK"
}