// TARGET_BACKEND: JVM_IR
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// WITH_STDLIB
fun <T> T.id() = this

val compareTo1 = 1u.<!EVALUATED{IR}("-1")!>compareTo(2u)<!>
val compareTo2 = 2u.<!EVALUATED{IR}("0")!>compareTo(2u)<!>
val compareTo3 = 3u.<!EVALUATED{IR}("1")!>compareTo(2u)<!>
val compareTo4 = 2u.<!EVALUATED{IR}("0")!>compareTo(2u.toUByte())<!>
val compareTo5 = 2u.<!EVALUATED{IR}("0")!>compareTo(2u.toUShort())<!>
val compareTo6 = 2u.<!EVALUATED{IR}("0")!>compareTo(2UL)<!>

val plus1 = 1u.<!EVALUATED{IR}("3")!>plus(2u)<!>
val plus2 = 2u.<!EVALUATED{IR}("4")!>plus(2u)<!>
val plus3 = 3u.<!EVALUATED{IR}("5")!>plus(2u)<!>
val plus4 = 2u.<!EVALUATED{IR}("4")!>plus(2u.toUByte())<!>
val plus5 = 2u.<!EVALUATED{IR}("4")!>plus(2u.toUShort())<!>
val plus6 = 2u.<!EVALUATED{IR}("4")!>plus(2UL)<!>

val minus1 = 2u.<!EVALUATED{IR}("0")!>minus(2u)<!>
val minus2 = 3u.<!EVALUATED{IR}("1")!>minus(2u)<!>
val minus3 = 2u.<!EVALUATED{IR}("0")!>minus(2u.toUByte())<!>
val minus4 = 2u.<!EVALUATED{IR}("0")!>minus(2u.toUShort())<!>
val minus5 = 2u.<!EVALUATED{IR}("0")!>minus(2u.toULong())<!>

val times1 = 1u.<!EVALUATED{IR}("2")!>times(2u)<!>
val times2 = 2u.<!EVALUATED{IR}("4")!>times(2u)<!>
val times3 = 3u.<!EVALUATED{IR}("6")!>times(2u)<!>
val times4 = 2u.<!EVALUATED{IR}("4")!>times(2u.toUByte())<!>
val times5 = 2u.<!EVALUATED{IR}("4")!>times(2u.toUShort())<!>
val times6 = 2u.<!EVALUATED{IR}("4")!>times(2u.toULong())<!>

val div1 = 1u.<!EVALUATED{IR}("0")!>div(2u)<!>
val div2 = 2u.<!EVALUATED{IR}("1")!>div(2u)<!>
val div3 = 3u.<!EVALUATED{IR}("1")!>div(2u)<!>
val div4 = 2u.<!EVALUATED{IR}("1")!>div(2u.toUByte())<!>
val div5 = 2u.<!EVALUATED{IR}("1")!>div(2u.toUShort())<!>
val div6 = 2u.<!EVALUATED{IR}("1")!>div(2u.toULong())<!>

val floorDiv1 = 1u.<!EVALUATED{IR}("0")!>floorDiv(2u)<!>
val floorDiv2 = 2u.<!EVALUATED{IR}("1")!>floorDiv(2u)<!>
val floorDiv3 = 3u.<!EVALUATED{IR}("1")!>floorDiv(2u)<!>
val floorDiv4 = 2u.<!EVALUATED{IR}("1")!>floorDiv(2u.toUByte())<!>
val floorDiv5 = 2u.<!EVALUATED{IR}("1")!>floorDiv(2u.toUShort())<!>
val floorDiv6 = 2u.<!EVALUATED{IR}("1")!>floorDiv(2u.toULong())<!>

val rem1 = 1u.<!EVALUATED{IR}("1")!>rem(2u)<!>
val rem2 = 2u.<!EVALUATED{IR}("0")!>rem(2u)<!>
val rem3 = 3u.<!EVALUATED{IR}("1")!>rem(2u)<!>
val rem4 = 2u.<!EVALUATED{IR}("0")!>rem(2u.toUByte())<!>
val rem5 = 2u.<!EVALUATED{IR}("0")!>rem(2u.toUShort())<!>
val rem6 = 2u.<!EVALUATED{IR}("0")!>rem(2u.toULong())<!>

val mod1 = 1u.<!EVALUATED{IR}("1")!>mod(2u)<!>
val mod2 = 2u.<!EVALUATED{IR}("0")!>mod(2u)<!>
val mod3 = 3u.<!EVALUATED{IR}("1")!>mod(2u)<!>
val mod4 = 2u.<!EVALUATED{IR}("0")!>mod(2u.toUByte())<!>
val mod5 = 2u.<!EVALUATED{IR}("0")!>mod(2u.toUShort())<!>
val mod6 = 2u.<!EVALUATED{IR}("0")!>mod(2u.toULong())<!>

val and1 = 1u.<!EVALUATED{IR}("1")!>and(1u)<!>
val and2 = 2u.<!EVALUATED{IR}("2")!>and(2u)<!>
val and3 = 3u.<!EVALUATED{IR}("2")!>and(2u)<!>
val and4 = 12u.<!EVALUATED{IR}("8")!>and(10u)<!>

val or1 = 1u.<!EVALUATED{IR}("1")!>or(1u)<!>
val or2 = 2u.<!EVALUATED{IR}("2")!>or(2u)<!>
val or3 = 3u.<!EVALUATED{IR}("3")!>or(2u)<!>
val or4 = 12u.<!EVALUATED{IR}("14")!>or(10u)<!>

val xor1 = 1u.<!EVALUATED{IR}("0")!>xor(1u)<!>
val xor2 = 2u.<!EVALUATED{IR}("0")!>xor(2u)<!>
val xor3 = 3u.<!EVALUATED{IR}("1")!>xor(2u)<!>
val xor4 = 12u.<!EVALUATED{IR}("6")!>xor(10u)<!>

val inv1 = 0u.<!EVALUATED{IR}("4294967295")!>inv()<!>
val inv2 = 1u.<!EVALUATED{IR}("4294967294")!>inv()<!>

val shl1 = 1u.<!EVALUATED{IR}("2")!>shl(1)<!>
val shl2 = 2u.<!EVALUATED{IR}("8")!>shl(2)<!>
val shl3 = 3u.<!EVALUATED{IR}("12")!>shl(2)<!>
val shl4 = 1u.<!EVALUATED{IR}("2147483648")!>shl(31)<!>
val shl5 = 1u.<!EVALUATED{IR}("1")!>shl(32)<!>
val shl6 = 1u.<!EVALUATED{IR}("2147483648")!>shl(63)<!>

val shr1 = 1u.<!EVALUATED{IR}("0")!>shr(1)<!>
val shr2 = 2u.<!EVALUATED{IR}("1")!>shr(1)<!>
val shr3 = 3u.<!EVALUATED{IR}("1")!>shr(1)<!>
val shr4 = 1u.<!EVALUATED{IR}("0")!>shr(31)<!>
val shr5 = 1u.<!EVALUATED{IR}("1")!>shr(32)<!>
val shr6 = 1u.<!EVALUATED{IR}("0")!>shr(63)<!>

val convert1 = 1u.<!EVALUATED{IR}("1")!>toUByte()<!>
val convert2 = 1u.<!EVALUATED{IR}("1")!>toUShort()<!>
val convert3 = 1u.<!EVALUATED{IR}("1")!>toUInt()<!>
val convert4 = 1u.<!EVALUATED{IR}("1")!>toULong()<!>
val convert5 = 1u.<!EVALUATED{IR}("1.0")!>toFloat()<!>
val convert6 = 1u.<!EVALUATED{IR}("1.0")!>toDouble()<!>
val convert7 = 1u.<!EVALUATED{IR}("1")!>toByte()<!>
val convert8 = 1u.<!EVALUATED{IR}("1")!>toShort()<!>
val convert9 = 1u.<!EVALUATED{IR}("1")!>toInt()<!>
val convert10 = 1u.<!EVALUATED{IR}("1")!>toLong()<!>

val equals1 = <!EVALUATED{IR}("false")!>1u == 2u<!>
val equals2 = <!EVALUATED{IR}("true")!>2u == 2u<!>
val equals3 = <!EVALUATED{IR}("false")!>3u == 2u<!>
val equals4 = <!EVALUATED{IR}("false")!>4u == 2u<!>

val toString1 = 1u.<!EVALUATED{IR}("1")!>toString()<!>
val toString2 = 2u.<!EVALUATED{IR}("2")!>toString()<!>

val limits1 = <!EVALUATED{IR}("4294967295")!>4294967294u+1u<!>
val limits2 = <!EVALUATED{IR}("0")!>4294967295u+1u<!>
val limits3 = <!EVALUATED{IR}("4294967295")!>0u-1u<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail compareTo 1"
    if (compareTo2.id() != 0)    return "Fail compareTo2"
    if (compareTo3.id() != 1)    return "Fail compareTo3"
    if (compareTo4.id() != 0)    return "Fail compareTo4"
    if (compareTo5.id() != 0)    return "Fail compareTo5"
    if (compareTo6.id() != 0)    return "Fail compareTo6"

    if (plus1.id() != 3u)     return "Fail plus1"
    if (plus2.id() != 4u)       return "Fail plus2"
    if (plus3.id() != 5u)     return "Fail plus3"
    if (plus4.id() != 4u)     return "Fail plus4"
    if (plus5.id() != 4u)     return "Fail plus5"
    if (plus6.id() != 4UL)    return "Fail plus6"

    if (minus1.id() != 0u)        return "Fail minus1"
    if (minus2.id() != 1u)        return "Fail minus2"
    if (minus3.id() != 0u)        return "Fail minus3"
    if (minus4.id() != 0u)        return "Fail minus4"
    if (minus5.id() != 0UL)        return "Fail minus5"

    if (times1.id() != 2u)        return "Fail times1"
    if (times2.id() != 4u)        return "Fail times2"
    if (times3.id() != 6u)        return "Fail times3"
    if (times4.id() != 4u)        return "Fail times4"
    if (times5.id() != 4u)        return "Fail times5"
    if (times6.id() != 4UL)       return "Fail times6"

    if (div1.id() != 0u)          return "Fail div1"
    if (div2.id() != 1u)          return "Fail div2"
    if (div3.id() != 1u)          return "Fail div3"
    if (div4.id() != 1u)          return "Fail div4"
    if (div5.id() != 1u)          return "Fail div5"
    if (div6.id() != 1uL)         return "Fail div6"

    if (floorDiv1.id() != 0u)          return "Fail  floorDiv1"
    if (floorDiv2.id() != 1u)          return "Fail  floorDiv2"
    if (floorDiv3.id() != 1u)          return "Fail  floorDiv3"
    if (floorDiv4.id() != 1u)          return "Fail  floorDiv4"
    if (floorDiv5.id() != 1u)          return "Fail  floorDiv5"
    if (floorDiv6.id() != 1uL)         return "Fail  floorDiv6"

    if (rem1.id() != 1u)      return "Fail rem1"
    if (rem2.id() != 0u)      return "Fail rem2"
    if (rem3.id() != 1u)      return "Fail rem3"
    if (rem4.id() != 0u)      return "Fail rem4"
    if (rem5.id() != 0u)      return "Fail rem5"
    if (rem6.id() != 0UL)     return "Fail rem6"

    if (mod1.id() != 1u)                return "Fail mod1"
    if (mod2.id() != 0u)                return "Fail mod2"
    if (mod3.id() != 1u)                return "Fail mod3"
    if (mod4.id() != 0u.toUByte())      return "Fail mod4"
    if (mod5.id() != 0u.toUShort())     return "Fail mod5"
    if (mod6.id() != 0UL)               return "Fail mod6"

    if (and1.id() != 1u)      return "Fail and1"
    if (and2.id() != 2u)      return "Fail and2"
    if (and3.id() != 2u)      return "Fail and3"
    if (and4.id() != 8u)      return "Fail and4"

    if (or1.id() != 1u)      return "Fail or1"
    if (or2.id() != 2u)      return "Fail or2"
    if (or3.id() != 3u)      return "Fail or3"
    if (or4.id() != 14u)     return "Fail or4"

    if (xor1.id() != 0u)      return "Fail xor1"
    if (xor2.id() != 0u)      return "Fail xor2"
    if (xor3.id() != 1u)      return "Fail xor3"
    if (xor4.id() != 6u)      return "Fail xor4"

    if (inv1.id() != 4294967295u)      return "Fail inv1"
    if (inv2.id() != 4294967294u)      return "Fail inv2"

    if (shl1.id() != 2u)                return "Fail shl1"
    if (shl2.id() != 8u)                return "Fail shl2"
    if (shl3.id() != 12u)               return "Fail shl3"
    if (shl4.id() != 2147483648u)       return "Fail shl4"
    if (shl5.id() != 1u)                return "Fail shl5"
    if (shl6.id() != 2147483648u)       return "Fail shl6"

    if (shr1.id() != 0u)                return "Fail shr1"
    if (shr2.id() != 1u)                return "Fail shr2"
    if (shr3.id() != 1u)                return "Fail shr3"
    if (shr4.id() != 0u)                return "Fail shr4"
    if (shr5.id() != 1u)                return "Fail shr5"
    if (shr6.id() != 0u)                return "Fail shr6"

    if (convert1.id() != 1u.toUByte())   return "Fail convert1"
    if (convert2.id() != 1u.toUShort())  return "Fail convert2"
    if (convert3.id() != 1u)             return "Fail convert3"
    if (convert4.id() != 1UL)            return "Fail convert4"
    if (convert7.id() != 1.toByte())     return "Fail convert7"
    if (convert8.id() != 1.toShort())    return "Fail convert8"
    if (convert9.id() != 1)              return "Fail convert9"
    if (convert10.id() != 1L)            return "Fail convert10"

    if (equals1.id() != false)   return "Fail equals1"
    if (equals2.id() != true)    return "Fail equals2"
    if (equals3.id() != false)   return "Fail equals3"
    if (equals4.id() != false)   return "Fail equals4"

    if (toString1.id() != "1")   return "Fail toString1"
    if (toString2.id() != "2")   return "Fail toString2"

    if (limits1.id() != 4294967295u)   return "Fail limits1"
    if (limits2.id() != 0u)            return "Fail limits2"
    if (limits3.id() != 4294967295u)   return "Fail limits3"

    return "OK"
}
