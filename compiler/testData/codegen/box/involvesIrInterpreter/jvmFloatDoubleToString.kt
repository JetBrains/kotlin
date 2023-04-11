// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
fun <T> T.id() = this

const val toStringDouble1 = 1.0.<!EVALUATED("1.0")!>toString()<!>
const val toStringDouble2 = 2.0.<!EVALUATED("2.0")!>toString()<!>
const val toStringDouble3 = 1.5.<!EVALUATED("1.5")!>toString()<!>

const val toStringFloat1 = 1.0f.<!EVALUATED("1.0")!>toString()<!>
const val toStringFloat2 = 2.0f.<!EVALUATED("2.0")!>toString()<!>
const val toStringFloat3 = 1.5f.<!EVALUATED("1.5")!>toString()<!>

fun box(): String {
    if (<!EVALUATED("1.0")!>toStringDouble1<!>.id() != "1.0")    return "Fail 1.1"
    if (<!EVALUATED("2.0")!>toStringDouble2<!>.id() != "2.0")    return "Fail 1.2"
    if (<!EVALUATED("1.5")!>toStringDouble3<!>.id() != "1.5")    return "Fail 1.3"

    if (<!EVALUATED("1.0")!>toStringFloat1<!>.id() != "1.0")     return "Fail 2.1"
    if (<!EVALUATED("2.0")!>toStringFloat2<!>.id() != "2.0")     return "Fail 2.2"
    if (<!EVALUATED("1.5")!>toStringFloat3<!>.id() != "1.5")     return "Fail 2.3"

    val localDoubleToString = 1.0.<!EVALUATED("1.0")!>toString()<!>
    val localFloatToString = 1.0f.<!EVALUATED("1.0")!>toString()<!>
    if (localDoubleToString.id() != <!EVALUATED("1.0")!>toStringDouble1<!>)    return "Fail 3.1"
    if (localFloatToString.id() != <!EVALUATED("1.0")!>toStringFloat1<!>)      return "Fail 3.2"

    return "OK"
}
