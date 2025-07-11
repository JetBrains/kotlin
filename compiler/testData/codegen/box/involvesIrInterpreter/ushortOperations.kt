// WITH_STDLIB
fun <T> T.id() = this

val compareTo1 = 1u.toUShort().<!EVALUATED{IR}("-1")!>compareTo(2u.toUShort())<!>
val compareTo2 = 2u.toUShort().<!EVALUATED{IR}("0")!>compareTo(2u.toUShort())<!>
val compareTo3 = 3u.toUShort().<!EVALUATED{IR}("1")!>compareTo(2u.toUShort())<!>
val compareTo4 = 2u.toUShort().<!EVALUATED{IR}("0")!>compareTo(2u.toUByte())<!>
val compareTo5 = 2u.toUShort().<!EVALUATED{IR}("0")!>compareTo(2u)<!>
val compareTo6 = 2u.toUShort().<!EVALUATED{IR}("0")!>compareTo(2UL)<!>


val plus1 = 1u.toUShort().<!EVALUATED{IR}("3")!>plus(2u.toUShort())<!>
val plus2 = 2u.toUShort().<!EVALUATED{IR}("4")!>plus(2u.toUShort())<!>
val plus3 = 3u.toUShort().<!EVALUATED{IR}("5")!>plus(2u.toUShort())<!>
val plus4 = 2u.toUShort().<!EVALUATED{IR}("4")!>plus(2u.toUByte())<!>
val plus5 = 2u.toUShort().<!EVALUATED{IR}("4")!>plus(2u)<!>
val plus6 = 2u.toUShort().<!EVALUATED{IR}("4")!>plus(2UL)<!>

val minus1 = 2u.toUShort().<!EVALUATED{IR}("0")!>minus(2u.toUShort())<!>
val minus2 = 3u.toUShort().<!EVALUATED{IR}("1")!>minus(2u.toUShort())<!>
val minus3 = 2u.toUShort().<!EVALUATED{IR}("0")!>minus(2u.toUByte())<!>
val minus4 = 2u.toUShort().<!EVALUATED{IR}("0")!>minus(2u)<!>
val minus5 = 2u.toUShort().<!EVALUATED{IR}("0")!>minus(2u.toULong())<!>

val times1 = 1u.toUShort().<!EVALUATED{IR}("2")!>times(2u.toUShort())<!>
val times2 = 2u.toUShort().<!EVALUATED{IR}("4")!>times(2u.toUShort())<!>
val times3 = 3u.toUShort().<!EVALUATED{IR}("6")!>times(2u.toUShort())<!>
val times4 = 2u.toUShort().<!EVALUATED{IR}("4")!>times(2u.toUByte())<!>
val times5 = 2u.toUShort().<!EVALUATED{IR}("4")!>times(2u)<!>
val times6 = 2u.toUShort().<!EVALUATED{IR}("4")!>times(2u.toULong())<!>

val div1 = 1u.toUShort().<!EVALUATED{IR}("0")!>div(2u.toUShort())<!>
val div2 = 2u.toUShort().<!EVALUATED{IR}("1")!>div(2u.toUShort())<!>
val div3 = 3u.toUShort().<!EVALUATED{IR}("1")!>div(2u.toUShort())<!>
val div4 = 2u.toUShort().<!EVALUATED{IR}("1")!>div(2u.toUByte())<!>
val div5 = 2u.toUShort().<!EVALUATED{IR}("1")!>div(2u)<!>
val div6 = 2u.toUShort().<!EVALUATED{IR}("1")!>div(2u.toULong())<!>

val rem1 = 1u.toUShort().<!EVALUATED{IR}("1")!>rem(2u.toUShort())<!>
val rem2 = 2u.toUShort().<!EVALUATED{IR}("0")!>rem(2u.toUShort())<!>
val rem3 = 3u.toUShort().<!EVALUATED{IR}("1")!>rem(2u.toUShort())<!>
val rem4 = 2u.toUShort().<!EVALUATED{IR}("0")!>rem(2u.toUByte())<!>
val rem5 = 2u.toUShort().<!EVALUATED{IR}("0")!>rem(2u)<!>
val rem6 = 2u.toUShort().<!EVALUATED{IR}("0")!>rem(2u.toULong())<!>

val convert1 = 1u.toUShort().<!EVALUATED{IR}("1")!>toUByte()<!>
val convert2 = 1u.toUShort().<!EVALUATED{IR}("1")!>toUShort()<!>
val convert3 = 1u.toUShort().<!EVALUATED{IR}("1")!>toUInt()<!>
val convert4 = 1u.toUShort().<!EVALUATED{IR}("1")!>toULong()<!>
val convert5 = 1u.toUShort().<!EVALUATED{IR}("1.0")!>toFloat()<!>
val convert6 = 1u.toUShort().<!EVALUATED{IR}("1.0")!>toDouble()<!>
val convert7 = 1u.toUShort().<!EVALUATED{IR}("1")!>toByte()<!>
val convert8 = 1u.toUShort().<!EVALUATED{IR}("1")!>toShort()<!>
val convert9 = 1u.toUShort().<!EVALUATED{IR}("1")!>toInt()<!>
val convert10 = 1u.toUShort().<!EVALUATED{IR}("1")!>toLong()<!>

val equals1 = <!EVALUATED{IR}("false")!>1u.toUShort() == 2u.toUShort()<!>
val equals2 = <!EVALUATED{IR}("true")!>2u.toUShort() == 2u.toUShort()<!>
val equals3 = <!EVALUATED{IR}("false")!>3u.toUShort() == 2u.toUShort()<!>
val equals4 = <!EVALUATED{IR}("false")!>4u.toUShort() == 2u.toUShort()<!>

val toString1 = 1u.toUShort().<!EVALUATED{IR}("1")!>toString()<!>
val toString2 = 2u.toUShort().<!EVALUATED{IR}("2")!>toString()<!>

val limits1 = <!EVALUATED{IR}("65535")!>65534u.toUShort()+1u.toUShort()<!>
val limits2 = <!EVALUATED{IR}("65536")!>65535u.toUShort()+1u.toUShort()<!>
val limits3 = <!EVALUATED{IR}("4294967295")!>0u.toUShort()-1u.toUShort()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail 1.1"
    if (compareTo2.id() != 0)    return "Fail 1.2"
    if (compareTo3.id() != 1)    return "Fail 1.3"
    if (compareTo4.id() != 0)    return "Fail 1.4"
    if (compareTo5.id() != 0)    return "Fail 1.5"
    if (compareTo6.id() != 0)    return "Fail 1.6"

    if (plus1.id() != 3u)    return "Fail 2.1"
    if (plus2.id() != 4u)    return "Fail 2.2"
    if (plus3.id() != 5u)    return "Fail 2.3"
    if (plus4.id() != 4u)    return "Fail 2.4"
    if (plus5.id() != 4u)               return "Fail 2.5"
    if (plus6.id() != 4UL)              return "Fail 2.6"

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

    if (limits1.id() != 65535u)      return "Fail 11.1"
    if (limits2.id() != 65536u)          return "Fail 11.2"
    if (limits3.id() != 4294967295u)      return "Fail 11.3"

    return "OK"
}
