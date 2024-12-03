// ISSUE: KT-73130

var trueCondition = true

fun box(): String {
    do {
        if (trueCondition) {
            break
        }
        return "FAIL"
    } while (trueCondition)

    return "OK"
}
