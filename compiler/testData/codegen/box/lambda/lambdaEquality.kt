// ISSUE: KT-61999

val lambda = { i: Int -> i }

fun testSameGlobal(): ((Int) -> Int) = lambda
fun testNoCapture(): ((Int) -> Int) = { i: Int -> i }
fun testCaptureParam(param: Int): ((Int) -> Int) = { i: Int -> param }

val globalIntVal = 0
fun testGlobalCaptureVal(): ((Int) -> Int) = { i: Int -> globalIntVal }
var globalIntVar = 0
fun testGlobalCaptureVar(): ((Int) -> Int) = { i: Int -> globalIntVar }

fun box(): String {
    if (testCaptureParam(42) == testCaptureParam(42))
        return "FAIL: lambdas from testCapture() must be distinct due to distinct captures"

    if (testSameGlobal() != testSameGlobal())
        return "FAIL: lambdas from testSameGlobal() must be equal, since it's the same instance"
    if (testNoCapture() != testNoCapture())
        return "FAIL: lambdas from testNoCapture() must be equal, since it's the same instance"
    if (testGlobalCaptureVal() != testGlobalCaptureVal())
        return "FAIL: lambdas from testGlobalCaptureVal() must be equal due to the same val capture"
    if (testGlobalCaptureVar() != testGlobalCaptureVar())
        return "FAIL: lambdas from testGlobalCaptureVar() must be equal due to the same capture, even if it's variable"
    return "OK"
}
