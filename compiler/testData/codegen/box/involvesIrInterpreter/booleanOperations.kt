// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// `Boolean.equals(Boolean)` will not be evaluated in K1
// IGNORE_BACKEND_K1: NATIVE

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
    if (<!EVALUATED("false")!>not1 != false<!>)  return "Fail 1.1"
    if (<!EVALUATED("false")!>not2 != true<!>)   return "Fail 1.2"

    if (<!EVALUATED("false")!>and1 != true<!>)   return "Fail 2.1"
    if (<!EVALUATED("false")!>and2 != false<!>)  return "Fail 2.2"
    if (<!EVALUATED("false")!>and3 != false<!>)  return "Fail 2.3"
    if (<!EVALUATED("false")!>and4 != false<!>)  return "Fail 2.4"

    if (<!EVALUATED("false")!>or1 != true<!>)    return "Fail 3.1"
    if (<!EVALUATED("false")!>or2 != true<!>)    return "Fail 3.2"
    if (<!EVALUATED("false")!>or3 != true<!>)    return "Fail 3.3"
    if (<!EVALUATED("false")!>or4 != false<!>)   return "Fail 3.4"

    if (<!EVALUATED("false")!>xor1 != false<!>)  return "Fail 4.1"
    if (<!EVALUATED("false")!>xor2 != true<!>)   return "Fail 4.2"
    if (<!EVALUATED("false")!>xor3 != true<!>)   return "Fail 4.3"
    if (<!EVALUATED("false")!>xor4 != false<!>)  return "Fail 4.4"

    if (<!EVALUATED("false")!>compareTo1 != 0<!>)    return "Fail 5.1"
    if (<!EVALUATED("false")!>compareTo2 != 1<!>)    return "Fail 5.2"
    if (<!EVALUATED("false")!>compareTo3 != -1<!>)   return "Fail 5.3"
    if (<!EVALUATED("false")!>compareTo4 != 0<!>)    return "Fail 5.4"

    if (<!EVALUATED("false")!>equals1 != true<!>)    return "Fail 6.1"
    if (<!EVALUATED("false")!>equals2 != false<!>)   return "Fail 6.2"
    if (<!EVALUATED("false")!>equals3 != false<!>)   return "Fail 6.3"
    if (<!EVALUATED("false")!>equals4 != true<!>)    return "Fail 6.4"

    if (<!EVALUATED("false")!>toString1 != "true"<!>)    return "Fail 7.1"
    if (<!EVALUATED("false")!>toString2 != "false"<!>)   return "Fail 7.2"
    return "OK"
}
