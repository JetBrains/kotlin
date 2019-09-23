fun fn_true() = true

fun fn_answer(): Int {
    if (fn_true()) {
        return 42
    } else {
        return 666
    }
}

fun box(): String {
    val answer = fn_answer()
    if (answer == 42) {
        return "OK"
    } else {
        return "FAIL"
    }
}