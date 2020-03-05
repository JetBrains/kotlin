// TARGET_BACKEND: JVM

var capturedLambda: ((Int) -> Int)? = null

fun captureLambda(): Boolean {
    val lambda = { x: Int -> x + 1 }
    if (capturedLambda == null) {
        capturedLambda = lambda
    } else if (capturedLambda !== lambda) {
        return false
    }
    return true
}

fun box(): String {
    captureLambda()
    if (!captureLambda())
        return "FAIL"
    return "OK"
}
