// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// WITH_STDLIB
fun <T> T.id() = this

const val code = '1'.<!EVALUATED("49")!>code<!>
const val floorDiv = 10.<!EVALUATED("5")!>floorDiv(2)<!>
const val mod = 5.<!EVALUATED("2")!>mod(3)<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (code.id() != 49) return "Fail 1"
    if (floorDiv.id() != 5) return "Fail 2"
    if (mod.id() != 2) return "Fail 3"

    return "OK"
}
