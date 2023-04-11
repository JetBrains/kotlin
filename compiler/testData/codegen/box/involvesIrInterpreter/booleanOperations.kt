// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// `Boolean.equals(Boolean)` will not be evaluated in K1
// IGNORE_BACKEND_K1: NATIVE
fun <T> T.id() = this

const val trueVal = <!EVALUATED("true")!>true<!>
const val falseVal = <!EVALUATED("false")!>false<!>

const val not1 = trueVal.<!EVALUATED("false")!>not()<!>
const val not2 = falseVal.<!EVALUATED("true")!>not()<!>

const val and1 = trueVal.<!EVALUATED("true")!>and(trueVal)<!>
const val and2 = trueVal.<!EVALUATED("false")!>and(falseVal)<!>
const val and3 = falseVal.<!EVALUATED("false")!>and(trueVal)<!>
const val and4 = falseVal.<!EVALUATED("false")!>and(falseVal)<!>

const val or1 = trueVal.<!EVALUATED("true")!>or(trueVal)<!>
const val or2 = trueVal.<!EVALUATED("true")!>or(falseVal)<!>
const val or3 = falseVal.<!EVALUATED("true")!>or(trueVal)<!>
const val or4 = falseVal.<!EVALUATED("false")!>or(falseVal)<!>

const val xor1 = trueVal.<!EVALUATED("false")!>xor(trueVal)<!>
const val xor2 = trueVal.<!EVALUATED("true")!>xor(falseVal)<!>
const val xor3 = falseVal.<!EVALUATED("true")!>xor(trueVal)<!>
const val xor4 = falseVal.<!EVALUATED("false")!>xor(falseVal)<!>

const val compareTo1 = trueVal.<!EVALUATED("0")!>compareTo(trueVal)<!>
const val compareTo2 = trueVal.<!EVALUATED("1")!>compareTo(falseVal)<!>
const val compareTo3 = falseVal.<!EVALUATED("-1")!>compareTo(trueVal)<!>
const val compareTo4 = falseVal.<!EVALUATED("0")!>compareTo(falseVal)<!>

const val equals1 = <!EVALUATED("true")!>trueVal == trueVal<!>
const val equals2 = <!EVALUATED("false")!>trueVal == falseVal<!>
const val equals3 = <!EVALUATED("false")!>falseVal == trueVal<!>
const val equals4 = <!EVALUATED("true")!>falseVal == falseVal<!>

const val toString1 = trueVal.<!EVALUATED("true")!>toString()<!>
const val toString2 = falseVal.<!EVALUATED("false")!>toString()<!>

fun box(): String {
    if (<!EVALUATED("false")!>not1<!>.id() != false)  return "Fail 1.1"
    if (<!EVALUATED("true")!>not2<!>.id() != true)   return "Fail 1.2"

    if (<!EVALUATED("true")!>and1<!>.id() != true)   return "Fail 2.1"
    if (<!EVALUATED("false")!>and2<!>.id() != false)  return "Fail 2.2"
    if (<!EVALUATED("false")!>and3<!>.id() != false)  return "Fail 2.3"
    if (<!EVALUATED("false")!>and4<!>.id() != false)  return "Fail 2.4"

    if (<!EVALUATED("true")!>or1<!>.id() != true)    return "Fail 3.1"
    if (<!EVALUATED("true")!>or2<!>.id() != true)    return "Fail 3.2"
    if (<!EVALUATED("true")!>or3<!>.id() != true)    return "Fail 3.3"
    if (<!EVALUATED("false")!>or4<!>.id() != false)   return "Fail 3.4"

    if (<!EVALUATED("false")!>xor1<!>.id() != false)  return "Fail 4.1"
    if (<!EVALUATED("true")!>xor2<!>.id() != true)   return "Fail 4.2"
    if (<!EVALUATED("true")!>xor3<!>.id() != true)   return "Fail 4.3"
    if (<!EVALUATED("false")!>xor4<!>.id() != false)  return "Fail 4.4"

    if (<!EVALUATED("0")!>compareTo1<!>.id() != 0)    return "Fail 5.1"
    if (<!EVALUATED("1")!>compareTo2<!>.id() != 1)    return "Fail 5.2"
    if (<!EVALUATED("-1")!>compareTo3<!>.id() != -1)   return "Fail 5.3"
    if (<!EVALUATED("0")!>compareTo4<!>.id() != 0)    return "Fail 5.4"

    if (<!EVALUATED("true")!>equals1<!>.id() != true)    return "Fail 6.1"
    if (<!EVALUATED("false")!>equals2<!>.id() != false)   return "Fail 6.2"
    if (<!EVALUATED("false")!>equals3<!>.id() != false)   return "Fail 6.3"
    if (<!EVALUATED("true")!>equals4<!>.id() != true)    return "Fail 6.4"

    if (<!EVALUATED("true")!>toString1<!>.id() != "true")    return "Fail 7.1"
    if (<!EVALUATED("false")!>toString2<!>.id() != "false")   return "Fail 7.2"
    return "OK"
}
