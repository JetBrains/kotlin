// WITH_STDLIB
fun <T> T.id() = this

const val oneVal = <!EVALUATED("1")!>'1'<!>
const val twoVal = <!EVALUATED("2")!>'2'<!>
const val threeVal = <!EVALUATED("3")!>'3'<!>
const val fourVal = <!EVALUATED("4")!>'4'<!>

const val intVal = <!EVALUATED("5")!>5<!>

const val compareTo1 = <!EVALUATED{IR}("-1")!>oneVal.<!EVALUATED{FIR}("-1")!>compareTo(twoVal)<!><!>
const val compareTo2 = <!EVALUATED{IR}("0")!>twoVal.<!EVALUATED{FIR}("0")!>compareTo(twoVal)<!><!>
const val compareTo3 = <!EVALUATED{IR}("1")!>threeVal.<!EVALUATED{FIR}("1")!>compareTo(twoVal)<!><!>
const val compareTo4 = <!EVALUATED{IR}("1")!>fourVal.<!EVALUATED{FIR}("1")!>compareTo(twoVal)<!><!>

const val plus1 = <!EVALUATED{IR}("6")!>oneVal.<!EVALUATED{FIR}("6")!>plus(intVal)<!><!>
const val plus2 = <!EVALUATED{IR}("7")!>twoVal.<!EVALUATED{FIR}("7")!>plus(intVal)<!><!>
const val plus3 = <!EVALUATED{IR}("8")!>threeVal.<!EVALUATED{FIR}("8")!>plus(intVal)<!><!>
const val plus4 = <!EVALUATED{IR}("9")!>fourVal.<!EVALUATED{FIR}("9")!>plus(intVal)<!><!>

const val minusChar1 = <!EVALUATED{IR}("-1")!>oneVal.<!EVALUATED{FIR}("-1")!>minus(twoVal)<!><!>
const val minusChar2 = <!EVALUATED{IR}("0")!>twoVal.<!EVALUATED{FIR}("0")!>minus(twoVal)<!><!>
const val minusChar3 = <!EVALUATED{IR}("1")!>threeVal.<!EVALUATED{FIR}("1")!>minus(twoVal)<!><!>
const val minusChar4 = <!EVALUATED{IR}("2")!>fourVal.<!EVALUATED{FIR}("2")!>minus(twoVal)<!><!>

const val minusInt1 = <!EVALUATED{IR}(",")!>oneVal.<!EVALUATED{FIR}(",")!>minus(intVal)<!><!>
const val minusInt2 = <!EVALUATED{IR}("-")!>twoVal.<!EVALUATED{FIR}("-")!>minus(intVal)<!><!>
const val minusInt3 = <!EVALUATED{IR}(".")!>threeVal.<!EVALUATED{FIR}(".")!>minus(intVal)<!><!>
const val minusInt4 = <!EVALUATED{IR}("/")!>fourVal.<!EVALUATED{FIR}("/")!>minus(intVal)<!><!>

const val convert1 = <!EVALUATED{IR}("49")!>oneVal.<!EVALUATED{FIR}("49")!>toByte()<!><!>
const val convert2 = <!EVALUATED{IR}("1")!>oneVal.<!EVALUATED{FIR}("1")!>toChar()<!><!>
const val convert3 = <!EVALUATED{IR}("49")!>oneVal.<!EVALUATED{FIR}("49")!>toShort()<!><!>
const val convert4 = <!EVALUATED{IR}("49")!>oneVal.<!EVALUATED{FIR}("49")!>toInt()<!><!>
const val convert5 = <!EVALUATED{IR}("49")!>oneVal.<!EVALUATED{FIR}("49")!>toLong()<!><!>
const val convert6 = <!EVALUATED{IR}("49.0")!>oneVal.<!EVALUATED{FIR}("49.0")!>toFloat()<!><!>
const val convert7 = <!EVALUATED{IR}("49.0")!>oneVal.<!EVALUATED{FIR}("49.0")!>toDouble()<!><!>

const val equals1 = <!EVALUATED("false")!>oneVal == twoVal<!>
const val equals2 = <!EVALUATED("true")!>twoVal == twoVal<!>
const val equals3 = <!EVALUATED("false")!>threeVal == twoVal<!>
const val equals4 = <!EVALUATED("false")!>fourVal == twoVal<!>

const val toString1 = <!EVALUATED{IR}("1")!>oneVal.<!EVALUATED{FIR}("1")!>toString()<!><!>
const val toString2 = <!EVALUATED{IR}("2")!>twoVal.<!EVALUATED{FIR}("2")!>toString()<!><!>

const val code1 = <!EVALUATED{IR}("49")!>oneVal.<!EVALUATED{FIR}("49")!>code<!><!>
const val code2 = <!EVALUATED{IR}("50")!>twoVal.<!EVALUATED{FIR}("50")!>code<!><!>
const val code3 = <!EVALUATED{IR}("51")!>threeVal.<!EVALUATED{FIR}("51")!>code<!><!>
const val code4 = <!EVALUATED{IR}("52")!>fourVal.<!EVALUATED{FIR}("52")!>code<!><!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail 1.1"
    if (compareTo2.id() != 0)    return "Fail 1.2"
    if (compareTo3.id() != 1)    return "Fail 1.3"
    if (compareTo4.id() != 1)    return "Fail 1.4"

    if (plus1.id() != '6')   return "Fail 2.1"
    if (plus2.id() != '7')   return "Fail 2.2"
    if (plus3.id() != '8')   return "Fail 2.3"
    if (plus4.id() != '9')   return "Fail 2.4"

    if (minusChar1.id() != -1)   return "Fail 3.1"
    if (minusChar2.id() != 0)    return "Fail 3.2"
    if (minusChar3.id() != 1)    return "Fail 3.3"
    if (minusChar4.id() != 2)    return "Fail 3.4"

    if (minusInt1.id() != ',')   return "Fail 4.1"
    if (minusInt2.id() != '-')   return "Fail 4.2"
    if (minusInt3.id() != '.')   return "Fail 4.3"
    if (minusInt4.id() != '/')   return "Fail 4.4"

    if (convert1.id() != 49.toByte())    return "Fail 5.1"
    if (convert2.id() != '1')            return "Fail 5.2"
    if (convert3.id() != 49.toShort())   return "Fail 5.3"
    if (convert4.id() != 49)             return "Fail 5.4"
    if (convert5.id() != 49L)            return "Fail 5.5"
    if (convert6.id() != 49.0f)          return "Fail 5.6"
    if (convert7.id() != 49.0)           return "Fail 5.7"

    if (equals1.id() != false)   return "Fail 6.1"
    if (equals2.id() != true)    return "Fail 6.2"
    if (equals3.id() != false)   return "Fail 6.3"
    if (equals4.id() != false)   return "Fail 6.4"

    if (toString1.id() != "1")   return "Fail 7.1"
    if (toString2.id() != "2")   return "Fail 7.2"

    if (code1.id() != 49)   return "Fail 8.1"
    if (code2.id() != 50)   return "Fail 8.2"
    if (code3.id() != 51)   return "Fail 8.3"
    if (code4.id() != 52)   return "Fail 8.4"
    return "OK"
}
