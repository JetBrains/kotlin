// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: WASM
fun <T> T.id() = this

const val toStringDouble1 = 1.0.<!EVALUATED("1.0")!>toString()<!>
const val toStringDouble2 = 2.0.<!EVALUATED("2.0")!>toString()<!>
const val toStringDouble3 = 1.5.<!EVALUATED("1.5")!>toString()<!>

const val toStringFloat1 = 1.0f.<!EVALUATED("1.0")!>toString()<!>
const val toStringFloat2 = 2.0f.<!EVALUATED("2.0")!>toString()<!>
const val toStringFloat3 = 1.5f.<!EVALUATED("1.5")!>toString()<!>

fun box(): String {
    // STOP_EVALUATION_CHECKS
    if (toStringDouble1.id() != "1.0")  return "Fail 1.1"
    if (toStringDouble2.id() != "2.0")  return "Fail 1.2"
    if (toStringDouble3.id() != "1.5")  return "Fail 1.3"

    if (toStringFloat1.id() != "1.0")   return "Fail 2.1"
    if (toStringFloat2.id() != "2.0")   return "Fail 2.2"
    if (toStringFloat3.id() != "1.5")   return "Fail 2.3"

    return "OK"
}
