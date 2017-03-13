var result = "Fail"

fun setOK(): Boolean {
    result = "OK"
    return true
}

fun box(): String {
    if (setOK()) {
    } else {
    }
    return result
}
