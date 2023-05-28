// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

fun <T> T.id() = this

const val toStringDouble1 = 1.0.<!EVALUATED("1")!>toString()<!>
const val toStringDouble2 = 2.0.<!EVALUATED("2")!>toString()<!>
const val toStringDouble3 = 1.5.<!EVALUATED("1.5")!>toString()<!>

const val toStringFloat1 = 1.0f.<!EVALUATED("1")!>toString()<!>
const val toStringFloat2 = 2.0f.<!EVALUATED("2")!>toString()<!>
const val toStringFloat3 = 1.5f.<!EVALUATED("1.5")!>toString()<!>

fun box(): String {
    // STOP_EVALUATION_CHECKS
    if (toStringDouble1.id() != "1")    return "Fail 1.1"
    if (toStringDouble2.id() != "2")    return "Fail 1.2"
    if (toStringDouble3.id() != "1.5")  return "Fail 1.3"

    if (toStringFloat1.id() != "1")    return "Fail 2.1"
    if (toStringFloat2.id() != "2")    return "Fail 2.2"
    if (toStringFloat3.id() != "1.5")  return "Fail 2.3"

    // START_EVALUATION_CHECKS
    val localDoubleToString = 1.0.<!EVALUATED("1")!>toString()<!>
    val localFloatToString = 1.0f.<!EVALUATED("1")!>toString()<!>
    // STOP_EVALUATION_CHECKS

    if (localDoubleToString.id() != toStringDouble1)    return "Fail 3.1"
    if (localFloatToString.id() != toStringFloat1)     return "Fail 3.2"

    return "OK"
}
