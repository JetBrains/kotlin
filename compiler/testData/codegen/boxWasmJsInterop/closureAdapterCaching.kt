// TARGET_BACKEND: WASM

fun checkLambdaEquality(a: () -> Int, b: () -> Int): Boolean = js("a === b")

fun f1(): Int = 42
fun f2(): Int = 24

fun box(): String {
    val x = { 42 }
    val y = { 24 }

    if (!checkLambdaEquality(x, x)) return "FAIL1"
    if (checkLambdaEquality(x, y)) return "FAIL2"

    val f1Ref = ::f1
    val f2Ref = ::f2

    if (!checkLambdaEquality(f1Ref, f1Ref)) return "FAIL3"
    if (checkLambdaEquality(f1Ref, f2Ref)) return "FAIL4"

    return "OK"
}