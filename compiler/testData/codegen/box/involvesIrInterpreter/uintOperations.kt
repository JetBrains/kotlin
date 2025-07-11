// WITH_STDLIB
fun <T> T.id() = this

const val oneVal = <!EVALUATED("1")!>1u<!>
const val twoVal = <!EVALUATED("2")!>2u<!>
const val threeVal = <!EVALUATED("3")!>3u<!>
const val fourVal = <!EVALUATED("4")!>4u<!>

const val compareTo1 = oneVal.<!EVALUATEDtoUByte()("-1")!>compareTo(twoVal)<!>
const val compareTo2 = 2u.<!EVALUATEDtoUByte()("0")!>compareTo(2u)<!>
const val compareTo3 = 3u.<!EVALUATEDtoUByte()("1")!>compareTo(2u)<!>
const val compareTo4 = 2u.<!EVALUATEDtoUByte()("0")!>compareTo(2u.toUByte())<!>
const val compareTo5 = 2u.<!EVALUATEDtoUByte()("0")!>compareTo(2u.toUShort())<!>
const val compareTo6 = 2u.<!EVALUATEDtoUByte()("0")!>compareTo(2UL)<!>

const val plus1 = 1u.<!EVALUATEDtoUByte()("3")!>plus(2u)<!>
const val plus2 = 2u.<!EVALUATEDtoUByte()("4")!>plus(2u)<!>
const val plus3 = 3u.<!EVALUATEDtoUByte()("5")!>plus(2u)<!>
const val plus4 = 2u.<!EVALUATEDtoUByte()("4")!>plus(2u.toUByte())<!>
const val plus5 = 2u.<!EVALUATEDtoUByte()("4")!>plus(2u.toUShort())<!>
const val plus6 = 2u.<!EVALUATEDtoUByte()("4")!>plus(2UL)<!>

const val minus1 = 2u.<!EVALUATEDtoUByte()("0")!>minus(2u)<!>
const val minus2 = 3u.<!EVALUATEDtoUByte()("1")!>minus(2u)<!>
const val minus3 = 2u.<!EVALUATEDtoUByte()("0")!>minus(2u.toUByte())<!>
const val minus4 = 2u.<!EVALUATEDtoUByte()("0")!>minus(2u.toUShort())<!>
const val minus5 = 2u.<!EVALUATEDtoUByte()("0")!>minus(2u.toULong())<!>

const val times1 = 1u.<!EVALUATEDtoUByte()("2")!>times(2u)<!>
const val times2 = 2u.<!EVALUATEDtoUByte()("4")!>times(2u)<!>
const val times3 = 3u.<!EVALUATEDtoUByte()("6")!>times(2u)<!>
const val times4 = 2u.<!EVALUATEDtoUByte()("4")!>times(2u.toUByte())<!>
const val times5 = 2u.<!EVALUATEDtoUByte()("4")!>times(2u.toUShort())<!>
const val times6 = 2u.<!EVALUATEDtoUByte()("4")!>times(2u.toULong())<!>

const val div1 = 1u.<!EVALUATEDtoUByte()("0")!>div(2u)<!>
const val div2 = 2u.<!EVALUATEDtoUByte()("1")!>div(2u)<!>
const val div3 = 3u.<!EVALUATEDtoUByte()("1")!>div(2u)<!>
const val div4 = 2u.<!EVALUATEDtoUByte()("1")!>div(2u.toUByte())<!>
const val div5 = 2u.<!EVALUATEDtoUByte()("1")!>div(2u.toUShort())<!>
const val div6 = 2u.<!EVALUATEDtoUByte()("1")!>div(2u.toULong())<!>

const val rem1 = 1u.<!EVALUATEDtoUByte()("1")!>rem(2u)<!>
const val rem2 = 2u.<!EVALUATEDtoUByte()("0")!>rem(2u)<!>
const val rem3 = 3u.<!EVALUATEDtoUByte()("1")!>rem(2u)<!>
const val rem4 = 2u.<!EVALUATEDtoUByte()("0")!>rem(2u.toUByte())<!>
const val rem5 = 2u.<!EVALUATEDtoUByte()("0")!>rem(2u.toUShort())<!>
const val rem6 = 2u.<!EVALUATEDtoUByte()("0")!>rem(2u.toULong())<!>

const val convert1 = 1u.<!EVALUATEDtoUByte()("1")!>toUByte()<!>
const val convert2 = 1u.<!EVALUATEDtoUByte()("1")!>toUShort()<!>
const val convert3 = 1u.<!EVALUATEDtoUByte()("1")!>toUInt()<!>
const val convert4 = 1u.<!EVALUATEDtoUByte()("1")!>toULong()<!>
const val convert5 = 1u.<!EVALUATEDtoUByte()("1.0")!>toFloat()<!>
const val convert6 = 1u.<!EVALUATEDtoUByte()("1.0")!>toDouble()<!>
const val convert7 = 1u.<!EVALUATEDtoUByte()("1")!>toByte()<!>
const val convert8 = 1u.<!EVALUATEDtoUByte()("1")!>toShort()<!>
const val convert9 = 1u.<!EVALUATEDtoUByte()("1")!>toInt()<!>
const val convert10 = 1u.<!EVALUATEDtoUByte()("1")!>toLong()<!>

const val equals1 = <!EVALUATEDtoUByte()("false")!>1u == 2u<!>
const val equals2 = <!EVALUATEDtoUByte()("true")!>2u == 2u<!>
const val equals3 = <!EVALUATEDtoUByte()("false")!>3u == 2u<!>
const val equals4 = <!EVALUATEDtoUByte()("false")!>4u == 2u<!>

const val toString1 = 1u.<!EVALUATEDtoUByte()("1")!>toString()<!>
const val toString2 = 2u.<!EVALUATEDtoUByte()("2")!>toString()<!>

const val limits1 = <!EVALUATEDtoUByte()("4294967295")!>4294967294u+1u<!>
const val limits2 = <!EVALUATEDtoUByte()("0")!>4294967295u+1u<!>
const val limits3 = <!EVALUATEDtoUByte()("4294967295")!>0u-1u<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail 1.1"
    if (compareTo2.id() != 0)    return "Fail 1.2"
    if (compareTo3.id() != 1)    return "Fail 1.3"
    if (compareTo4.id() != 0)    return "Fail 1.4"
    if (compareTo5.id() != 0)    return "Fail 1.5"
    if (compareTo6.id() != 0)    return "Fail 1.6"

    if (plus1.id() != 3u)     return "Fail 2.1"
    if (plus2.id() != 4u)     return "Fail 2.2"
    if (plus3.id() != 5u)     return "Fail 2.3"
    if (plus4.id() != 4u)     return "Fail 2.4"
    if (plus5.id() != 4u)     return "Fail 2.5"
    if (plus6.id() != 4UL)    return "Fail 2.6"

    if (minus1.id() != 0u)        return "Fail 3.1"
    if (minus2.id() != 1u)        return "Fail 3.2"
    if (minus3.id() != 0u)        return "Fail 3.3"
    if (minus4.id() != 0u)        return "Fail 3.4"
    if (minus5.id() != 0UL)        return "Fail 3.5"

    if (times1.id() != 2u)        return "Fail 4.1"
    if (times2.id() != 4u)        return "Fail 4.2"
    if (times3.id() != 6u)        return "Fail 4.3"
    if (times4.id() != 4u)        return "Fail 4.4"
    if (times5.id() != 4u)        return "Fail 4.5"
    if (times6.id() != 4UL)       return "Fail 4.6"

    if (div1.id() != 0u)          return "Fail 5.1"
    if (div2.id() != 1u)          return "Fail 5.2"
    if (div3.id() != 1u)          return "Fail 5.3"
    if (div4.id() != 1u)          return "Fail 5.4"
    if (div5.id() != 1u)          return "Fail 5.5"
    if (div6.id() != 1uL)         return "Fail 5.6"

    if (rem1.id() != 1u)      return "Fail 6.1"
    if (rem2.id() != 0u)      return "Fail 6.2"
    if (rem3.id() != 1u)      return "Fail 6.3"
    if (rem4.id() != 0u)      return "Fail 6.4"
    if (rem5.id() != 0u)      return "Fail 6.5"
    if (rem6.id() != 0UL)     return "Fail 6.6"

    if (convert1.id() != 1u.toUByte())   return "Fail 8.1"
    if (convert2.id() != 1u.toUShort())  return "Fail 8.2"
    if (convert3.id() != 1u)             return "Fail 8.3"
    if (convert4.id() != 1UL)            return "Fail 8.4"
    if (convert7.id() != 1.toByte())     return "Fail 8.7"
    if (convert8.id() != 1.toShort())    return "Fail 8.8"
    if (convert9.id() != 1)              return "Fail 8.9"
    if (convert10.id() != 1L)            return "Fail 8.10"

    if (equals1.id() != false)   return "Fail 9.1"
    if (equals2.id() != true)    return "Fail 9.2"
    if (equals3.id() != false)   return "Fail 9.3"
    if (equals4.id() != false)   return "Fail 9.4"

    if (toString1.id() != "1")   return "Fail 10.1"
    if (toString2.id() != "2")   return "Fail 10.2"

    if (limits1.id() != 4294967295u)   return "Fail 11.1"
    if (limits2.id() != 0u)            return "Fail 11.2"
    if (limits3.id() != 4294967295u)   return "Fail 11.3"

    return "OK"
}
