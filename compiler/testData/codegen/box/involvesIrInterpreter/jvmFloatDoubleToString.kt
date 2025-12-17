// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73621: EVALUATED{IR} is missing
fun <T> T.id() = this

const val toStringDouble1 = <!EVALUATED{IR}("1.0")!>1.0.<!EVALUATED{FIR}("1.0")!>toString()<!><!>
const val toStringDouble2 = <!EVALUATED{IR}("2.0")!>2.0.<!EVALUATED{FIR}("2.0")!>toString()<!><!>
const val toStringDouble3 = <!EVALUATED{IR}("1.5")!>1.5.<!EVALUATED{FIR}("1.5")!>toString()<!><!>

const val toStringFloat1 = <!EVALUATED{IR}("1.0")!>1.0f.<!EVALUATED{FIR}("1.0")!>toString()<!><!>
const val toStringFloat2 = <!EVALUATED{IR}("2.0")!>2.0f.<!EVALUATED{FIR}("2.0")!>toString()<!><!>
const val toStringFloat3 = <!EVALUATED{IR}("1.5")!>1.5f.<!EVALUATED{FIR}("1.5")!>toString()<!><!>

fun box(): String {
    // STOP_EVALUATION_CHECKS
    if (toStringDouble1.id() != "1.0")    return "Fail 1.1"
    if (toStringDouble2.id() != "2.0")    return "Fail 1.2"
    if (toStringDouble3.id() != "1.5")    return "Fail 1.3"

    if (toStringFloat1.id() != "1.0")     return "Fail 2.1"
    if (toStringFloat2.id() != "2.0")     return "Fail 2.2"
    if (toStringFloat3.id() != "1.5")     return "Fail 2.3"

    // START_EVALUATION_CHECKS
    val localDoubleToString = 1.0.<!EVALUATED{IR}("1.0")!>toString()<!>
    val localFloatToString = 1.0f.<!EVALUATED{IR}("1.0")!>toString()<!>
    // STOP_EVALUATION_CHECKS

    if (localDoubleToString.id() != toStringDouble1)    return "Fail 3.1"
    if (localFloatToString.id() != toStringFloat1)      return "Fail 3.2"

    return "OK"
}
