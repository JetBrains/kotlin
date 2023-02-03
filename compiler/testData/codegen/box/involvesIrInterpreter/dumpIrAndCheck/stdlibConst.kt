// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB

const val code = '1'.code
const val floorDiv = 10.floorDiv(2)
const val mod = 5.mod(3)

fun box(): String {
    if (code != 49) return "Fail 1"
    if (floorDiv != 5) return "Fail 2"
    if (mod != 2) return "Fail 3"

    return "OK"
}