// FIR_IDENTICAL

fun foo(): Int {
    var result = 0
    try {
    } finally {
        42.let { }
    }
    return result
}
