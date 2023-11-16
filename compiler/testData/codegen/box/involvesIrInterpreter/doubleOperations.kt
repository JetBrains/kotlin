// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
fun <T> T.id() = this

const val minusOneVal = <!EVALUATED("-1.0")!>-1.0<!>
const val oneVal = <!EVALUATED("1.0")!>1.0<!>
const val twoVal = <!EVALUATED("2.0")!>2.0<!>
const val threeVal = <!EVALUATED("3.0")!>3.0<!>
const val fourVal = <!EVALUATED("4.0")!>4.0<!>
const val oneAndAHalf = <!EVALUATED("1.5")!>1.5<!>

const val byteVal = 2.<!EVALUATED("2")!>toByte()<!>
const val shortVal = 2.<!EVALUATED("2")!>toShort()<!>
const val intVal = <!EVALUATED("2")!>2<!>
const val longVal = <!EVALUATED("2")!>2L<!>
const val floatVal = <!EVALUATED("2.0")!>2.0f<!>
const val doubleVal = <!EVALUATED("2.0")!>2.0<!>

const val compareTo1 = oneVal.<!EVALUATED("-1")!>compareTo(twoVal)<!>
const val compareTo2 = twoVal.<!EVALUATED("0")!>compareTo(twoVal)<!>
const val compareTo3 = threeVal.<!EVALUATED("1")!>compareTo(twoVal)<!>
const val compareTo4 = twoVal.<!EVALUATED("0")!>compareTo(byteVal)<!>
const val compareTo5 = twoVal.<!EVALUATED("0")!>compareTo(shortVal)<!>
const val compareTo6 = twoVal.<!EVALUATED("0")!>compareTo(intVal)<!>
const val compareTo7 = twoVal.<!EVALUATED("0")!>compareTo(longVal)<!>
const val compareTo8 = twoVal.<!EVALUATED("0")!>compareTo(floatVal)<!>

const val plus1 = oneVal.<!EVALUATED("3.0")!>plus(twoVal)<!>
const val plus2 = twoVal.<!EVALUATED("4.0")!>plus(twoVal)<!>
const val plus3 = threeVal.<!EVALUATED("5.0")!>plus(twoVal)<!>
const val plus4 = twoVal.<!EVALUATED("4.0")!>plus(byteVal)<!>
const val plus5 = twoVal.<!EVALUATED("4.0")!>plus(shortVal)<!>
const val plus6 = twoVal.<!EVALUATED("4.0")!>plus(intVal)<!>
const val plus7 = twoVal.<!EVALUATED("4.0")!>plus(longVal)<!>
const val plus8 = twoVal.<!EVALUATED("4.0")!>plus(floatVal)<!>

const val minus1 = oneVal.<!EVALUATED("-1.0")!>minus(twoVal)<!>
const val minus2 = twoVal.<!EVALUATED("0.0")!>minus(twoVal)<!>
const val minus3 = threeVal.<!EVALUATED("1.0")!>minus(twoVal)<!>
const val minus4 = twoVal.<!EVALUATED("0.0")!>minus(byteVal)<!>
const val minus5 = twoVal.<!EVALUATED("0.0")!>minus(shortVal)<!>
const val minus6 = twoVal.<!EVALUATED("0.0")!>minus(intVal)<!>
const val minus7 = twoVal.<!EVALUATED("0.0")!>minus(longVal)<!>
const val minus8 = twoVal.<!EVALUATED("0.0")!>minus(floatVal)<!>

const val times1 = oneVal.<!EVALUATED("2.0")!>times(twoVal)<!>
const val times2 = twoVal.<!EVALUATED("4.0")!>times(twoVal)<!>
const val times3 = threeVal.<!EVALUATED("6.0")!>times(twoVal)<!>
const val times4 = twoVal.<!EVALUATED("4.0")!>times(byteVal)<!>
const val times5 = twoVal.<!EVALUATED("4.0")!>times(shortVal)<!>
const val times6 = twoVal.<!EVALUATED("4.0")!>times(intVal)<!>
const val times7 = twoVal.<!EVALUATED("4.0")!>times(longVal)<!>
const val times8 = twoVal.<!EVALUATED("4.0")!>times(floatVal)<!>

const val div1 = oneVal.<!EVALUATED("0.5")!>div(twoVal)<!>
const val div2 = twoVal.<!EVALUATED("1.0")!>div(twoVal)<!>
const val div3 = threeVal.<!EVALUATED("1.5")!>div(twoVal)<!>
const val div4 = twoVal.<!EVALUATED("1.0")!>div(byteVal)<!>
const val div5 = twoVal.<!EVALUATED("1.0")!>div(shortVal)<!>
const val div6 = twoVal.<!EVALUATED("1.0")!>div(intVal)<!>
const val div7 = twoVal.<!EVALUATED("1.0")!>div(longVal)<!>
const val div8 = twoVal.<!EVALUATED("1.0")!>div(floatVal)<!>

const val rem1 = oneVal.<!EVALUATED("1.0")!>rem(twoVal)<!>
const val rem2 = twoVal.<!EVALUATED("0.0")!>rem(twoVal)<!>
const val rem3 = threeVal.<!EVALUATED("1.0")!>rem(twoVal)<!>
const val rem4 = twoVal.<!EVALUATED("0.0")!>rem(byteVal)<!>
const val rem5 = twoVal.<!EVALUATED("0.0")!>rem(shortVal)<!>
const val rem6 = twoVal.<!EVALUATED("0.0")!>rem(intVal)<!>
const val rem7 = twoVal.<!EVALUATED("0.0")!>rem(longVal)<!>
const val rem8 = twoVal.<!EVALUATED("0.0")!>rem(floatVal)<!>

const val unaryPlus1 = oneVal.<!EVALUATED("1.0")!>unaryPlus()<!>
const val unaryPlus2 = minusOneVal.<!EVALUATED("-1.0")!>unaryPlus()<!>
const val unaryMinus1 = oneVal.<!EVALUATED("-1.0")!>unaryMinus()<!>
const val unaryMinus2 = minusOneVal.<!EVALUATED("1.0")!>unaryMinus()<!>

const val convert1 = oneVal.<!EVALUATED("")!>toChar()<!>
const val convert2 = oneVal.<!EVALUATED("1")!>toInt()<!>
const val convert3 = oneVal.<!EVALUATED("1")!>toLong()<!>
const val convert4 = oneVal.<!EVALUATED("1.0")!>toFloat()<!>
const val convert5 = oneVal.<!EVALUATED("1.0")!>toDouble()<!>

const val equals1 = <!EVALUATED("false")!>oneVal == twoVal<!>
const val equals2 = <!EVALUATED("true")!>twoVal == twoVal<!>
const val equals3 = <!EVALUATED("false")!>threeVal == twoVal<!>
const val equals4 = <!EVALUATED("false")!>fourVal == twoVal<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail 1.1"
    if (compareTo2.id() != 0)    return "Fail 1.2"
    if (compareTo3.id() != 1)    return "Fail 1.3"
    if (compareTo4.id() != 0)    return "Fail 1.4"
    if (compareTo5.id() != 0)    return "Fail 1.5"
    if (compareTo6.id() != 0)    return "Fail 1.6"
    if (compareTo7.id() != 0)    return "Fail 1.7"
    if (compareTo8.id() != 0)    return "Fail 1.8"

    if (plus1.id() != 3.0)      return "Fail 2.1"
    if (plus2.id() != 4.0)      return "Fail 2.2"
    if (plus3.id() != 5.0)      return "Fail 2.3"
    if (plus4.id() != 4.0)      return "Fail 2.4"
    if (plus5.id() != 4.0)      return "Fail 2.5"
    if (plus6.id() != 4.0)      return "Fail 2.6"
    if (plus7.id() != 4.0)      return "Fail 2.7"
    if (plus8.id() != 4.0)      return "Fail 2.8"

    if (minus1.id() != -1.0)    return "Fail 3.1"
    if (minus2.id() != 0.0)     return "Fail 3.2"
    if (minus3.id() != 1.0)     return "Fail 3.3"
    if (minus4.id() != 0.0)     return "Fail 3.4"
    if (minus5.id() != 0.0)     return "Fail 3.5"
    if (minus6.id() != 0.0)     return "Fail 3.6"
    if (minus7.id() != 0.0)     return "Fail 3.7"
    if (minus8.id() != 0.0)     return "Fail 3.8"

    if (times1.id() != 2.0)     return "Fail 4.1"
    if (times2.id() != 4.0)     return "Fail 4.2"
    if (times3.id() != 6.0)     return "Fail 4.3"
    if (times4.id() != 4.0)     return "Fail 4.4"
    if (times5.id() != 4.0)     return "Fail 4.5"
    if (times6.id() != 4.0)     return "Fail 4.6"
    if (times7.id() != 4.0)     return "Fail 4.7"
    if (times8.id() != 4.0)     return "Fail 4.8"

    if (div1.id() != 0.5)       return "Fail 5.1"
    if (div2.id() != 1.0)       return "Fail 5.2"
    if (div3.id() != 1.5)       return "Fail 5.3"
    if (div4.id() != 1.0)       return "Fail 5.4"
    if (div5.id() != 1.0)       return "Fail 5.5"
    if (div6.id() != 1.0)       return "Fail 5.6"
    if (div7.id() != 1.0)       return "Fail 5.7"
    if (div8.id() != 1.0)       return "Fail 5.8"

    if (rem1.id() != 1.0)       return "Fail 6.1"
    if (rem2.id() != 0.0)       return "Fail 6.2"
    if (rem3.id() != 1.0)       return "Fail 6.3"
    if (rem4.id() != 0.0)       return "Fail 6.4"
    if (rem5.id() != 0.0)       return "Fail 6.5"
    if (rem6.id() != 0.0)       return "Fail 6.6"
    if (rem7.id() != 0.0)       return "Fail 6.7"
    if (rem8.id() != 0.0)       return "Fail 6.8"

    if (unaryPlus1.id() != 1.0)    return "Fail 7.1"
    if (unaryPlus2.id() != -1.0)   return "Fail 7.2"
    if (unaryMinus1.id() != -1.0)  return "Fail 7.3"
    if (unaryMinus2.id() != 1.0)   return "Fail 7.4"

    if (convert1.id() != '')  return "Fail 8.1"
    if (convert2.id() != 1)      return "Fail 8.2"
    if (convert3.id() != 1L)     return "Fail 8.3"
    if (convert4.id() != 1.0f)   return "Fail 8.4"
    if (convert5.id() != 1.0)    return "Fail 8.5"

    if (equals1.id() != false)   return "Fail 9.1"
    if (equals2.id() != true)    return "Fail 9.2"
    if (equals3.id() != false)   return "Fail 9.3"
    if (equals4.id() != false)   return "Fail 9.4"

    return "OK"
}
