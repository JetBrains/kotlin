// TARGET_BACKEND: JVM

var capturedRef: ((Int) -> Int)? = null

fun ref(x: Int) = x

fun updateCapturedRef(): Boolean {
    val r = ::ref
    if (capturedRef == null) {
        capturedRef = r
    } else if (capturedRef !== r) {
        return false
    }
    return true
}

fun box(): String {
    updateCapturedRef()
    if (!updateCapturedRef())
        return "FAIL"
    return "OK"
}
