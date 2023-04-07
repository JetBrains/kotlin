// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// `Char.equals(Char)` will not be evaluated in K1
// IGNORE_BACKEND_K1: NATIVE
// WITH_STDLIB

const val oneVal = <!EVALUATED("1")!>'1'<!>
const val twoVal = <!EVALUATED("2")!>'2'<!>
const val threeVal = <!EVALUATED("3")!>'3'<!>
const val fourVal = <!EVALUATED("4")!>'4'<!>

const val intVal = <!EVALUATED("5")!>5<!>

const val compareTo1 = oneVal.<!EVALUATED("-1")!>compareTo(twoVal)<!>
const val compareTo2 = twoVal.<!EVALUATED("0")!>compareTo(twoVal)<!>
const val compareTo3 = threeVal.<!EVALUATED("1")!>compareTo(twoVal)<!>
const val compareTo4 = fourVal.<!EVALUATED("1")!>compareTo(twoVal)<!>

const val plus1 = oneVal.<!EVALUATED("6")!>plus(intVal)<!>
const val plus2 = twoVal.<!EVALUATED("7")!>plus(intVal)<!>
const val plus3 = threeVal.<!EVALUATED("8")!>plus(intVal)<!>
const val plus4 = fourVal.<!EVALUATED("9")!>plus(intVal)<!>

const val minusChar1 = oneVal.<!EVALUATED("-1")!>minus(twoVal)<!>
const val minusChar2 = twoVal.<!EVALUATED("0")!>minus(twoVal)<!>
const val minusChar3 = threeVal.<!EVALUATED("1")!>minus(twoVal)<!>
const val minusChar4 = fourVal.<!EVALUATED("2")!>minus(twoVal)<!>

const val minusInt1 = oneVal.<!EVALUATED(",")!>minus(intVal)<!>
const val minusInt2 = twoVal.<!EVALUATED("-")!>minus(intVal)<!>
const val minusInt3 = threeVal.<!EVALUATED(".")!>minus(intVal)<!>
const val minusInt4 = fourVal.<!EVALUATED("/")!>minus(intVal)<!>

const val convert1 = oneVal.<!EVALUATED("49")!>toByte()<!>
const val convert2 = oneVal.<!EVALUATED("1")!>toChar()<!>
const val convert3 = oneVal.<!EVALUATED("49")!>toShort()<!>
const val convert4 = oneVal.<!EVALUATED("49")!>toInt()<!>
const val convert5 = oneVal.<!EVALUATED("49")!>toLong()<!>
const val convert6 = oneVal.<!EVALUATED("49.0")!>toFloat()<!>
const val convert7 = oneVal.<!EVALUATED("49.0")!>toDouble()<!>

const val equals1 = <!EVALUATED("false")!>oneVal == twoVal<!>
const val equals2 = <!EVALUATED("true")!>twoVal == twoVal<!>
const val equals3 = <!EVALUATED("false")!>threeVal == twoVal<!>
const val equals4 = <!EVALUATED("false")!>fourVal == twoVal<!>

const val toString1 = oneVal.<!EVALUATED("1")!>toString()<!>
const val toString2 = twoVal.<!EVALUATED("2")!>toString()<!>

const val code1 = oneVal.<!EVALUATED("49")!>code<!>
const val code2 = twoVal.<!EVALUATED("50")!>code<!>
const val code3 = threeVal.<!EVALUATED("51")!>code<!>
const val code4 = fourVal.<!EVALUATED("52")!>code<!>

fun box(): String {
    if (<!EVALUATED("false")!>compareTo1 != -1<!>)   return "Fail 1.1"
    if (<!EVALUATED("false")!>compareTo2 != 0<!>)    return "Fail 1.2"
    if (<!EVALUATED("false")!>compareTo3 != 1<!>)    return "Fail 1.3"
    if (<!EVALUATED("false")!>compareTo4 != 1<!>)    return "Fail 1.4"

    if (<!EVALUATED("false")!>plus1 != '6'<!>)   return "Fail 2.1"
    if (<!EVALUATED("false")!>plus2 != '7'<!>)   return "Fail 2.2"
    if (<!EVALUATED("false")!>plus3 != '8'<!>)   return "Fail 2.3"
    if (<!EVALUATED("false")!>plus4 != '9'<!>)   return "Fail 2.4"

    if (<!EVALUATED("false")!>minusChar1 != -1<!>)   return "Fail 3.1"
    if (<!EVALUATED("false")!>minusChar2 != 0<!>)    return "Fail 3.2"
    if (<!EVALUATED("false")!>minusChar3 != 1<!>)    return "Fail 3.3"
    if (<!EVALUATED("false")!>minusChar4 != 2<!>)    return "Fail 3.4"

    if (<!EVALUATED("false")!>minusInt1 != ','<!>)   return "Fail 4.1"
    if (<!EVALUATED("false")!>minusInt2 != '-'<!>)   return "Fail 4.2"
    if (<!EVALUATED("false")!>minusInt3 != '.'<!>)   return "Fail 4.3"
    if (<!EVALUATED("false")!>minusInt4 != '/'<!>)   return "Fail 4.4"

    if (<!EVALUATED("false")!>convert1 != 49.toByte()<!>)    return "Fail 5.1"
    if (<!EVALUATED("false")!>convert2 != '1'<!>)            return "Fail 5.2"
    if (<!EVALUATED("false")!>convert3 != 49.toShort()<!>)   return "Fail 5.3"
    if (<!EVALUATED("false")!>convert4 != 49<!>)             return "Fail 5.4"
    if (<!EVALUATED("false")!>convert5 != 49L<!>)            return "Fail 5.5"
    if (<!EVALUATED("false")!>convert6 != 49.0f<!>)          return "Fail 5.6"
    if (<!EVALUATED("false")!>convert7 != 49.0<!>)           return "Fail 5.7"

    if (<!EVALUATED("false")!>equals1 != false<!>)   return "Fail 6.1"
    if (<!EVALUATED("false")!>equals2 != true<!>)    return "Fail 6.2"
    if (<!EVALUATED("false")!>equals3 != false<!>)   return "Fail 6.3"
    if (<!EVALUATED("false")!>equals4 != false<!>)   return "Fail 6.4"

    if (<!EVALUATED("false")!>toString1 != "1"<!>)   return "Fail 7.1"
    if (<!EVALUATED("false")!>toString2 != "2"<!>)   return "Fail 7.2"

    if (<!EVALUATED("false")!>code1 != 49<!>)   return "Fail 8.1"
    if (<!EVALUATED("false")!>code2 != 50<!>)   return "Fail 8.2"
    if (<!EVALUATED("false")!>code3 != 51<!>)   return "Fail 8.3"
    if (<!EVALUATED("false")!>code4 != 52<!>)   return "Fail 8.4"
    return "OK"
}
