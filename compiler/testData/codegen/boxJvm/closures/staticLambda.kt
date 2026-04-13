// TARGET_BACKEND: JVM

// INDY lambdas are not singletons on Android
// IGNORE_BACKEND: ANDROID

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
