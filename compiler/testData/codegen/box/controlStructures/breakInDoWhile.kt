// IGNORE_BACKEND: JS_IR, JS_IR_ES6
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
