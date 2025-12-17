// WITH_STDLIB
fun <T> T.id() = this

const val code = <!EVALUATED{IR}("49")!>'1'.<!EVALUATED{FIR}("49")!>code<!><!>
const val floorDiv = <!EVALUATED{IR}("5")!>10.<!EVALUATED{FIR}("5")!>floorDiv(2)<!><!>
const val mod = <!EVALUATED{IR}("2")!>5.<!EVALUATED{FIR}("2")!>mod(3)<!><!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (code.id() != 49) return "Fail 1"
    if (floorDiv.id() != 5) return "Fail 2"
    if (mod.id() != 2) return "Fail 3"

    return "OK"
}
