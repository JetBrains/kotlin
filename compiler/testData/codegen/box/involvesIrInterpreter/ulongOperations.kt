// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: ANY
// ^^^^^^^^^^^^^^^^^^^^^^ Unsigned operations will not be supported in the legacy K1 frontend.
// IGNORE_KLIB_1ST_PHASE_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0 2.3.0
// KT-51065: Fixed in 2.3.20-Beta1
// WITH_STDLIB
fun <T> T.id() = this

const val zeroVal = <!EVALUATED("0")!>0UL<!>
const val oneVal = <!EVALUATED("1")!>1UL<!>
const val twoVal = <!EVALUATED("2")!>2UL<!>
const val threeVal = <!EVALUATED("3")!>3UL<!>
const val fourVal = <!EVALUATED("4")!>4UL<!>

const val byteVal = 2.<!EVALUATED("2")!>toByte()<!>
const val shortVal = 2.<!EVALUATED("2")!>toShort()<!>
const val intVal = <!EVALUATED("2")!>2<!>
const val longVal = <!EVALUATED("2")!>2L<!>
const val ubyteVal = 2u.<!EVALUATED("2")!>toUByte()<!>
const val ushortVal = 2u.<!EVALUATED("2")!>toUShort()<!>
const val uintVal = <!EVALUATED("2")!>2u<!>
const val ulongVal = <!EVALUATED("2")!>2UL<!>
const val floatVal = <!EVALUATED("2.0")!>2.0f<!>
const val doubleVal = <!EVALUATED("2.0")!>2.0<!>

const val compareTo1 = oneVal.<!EVALUATED("-1")!>compareTo(twoVal)<!>
const val compareTo2 = twoVal.<!EVALUATED("0")!>compareTo(twoVal)<!>
const val compareTo3 = threeVal.<!EVALUATED("1")!>compareTo(twoVal)<!>
const val compareTo4 = twoVal.<!EVALUATED("0")!>compareTo(ubyteVal)<!>
const val compareTo5 = twoVal.<!EVALUATED("0")!>compareTo(ushortVal)<!>
const val compareTo6 = twoVal.<!EVALUATED("0")!>compareTo(uintVal)<!>

const val plus1 = oneVal.<!EVALUATED("3")!>plus(twoVal)<!>
const val plus2 = twoVal.<!EVALUATED("4")!>plus(twoVal)<!>
const val plus3 = threeVal.<!EVALUATED("5")!>plus(twoVal)<!>
const val plus4 = twoVal.<!EVALUATED("4")!>plus(ubyteVal)<!>
const val plus5 = twoVal.<!EVALUATED("4")!>plus(ushortVal)<!>
const val plus6 = twoVal.<!EVALUATED("4")!>plus(uintVal)<!>

const val minus1 = oneVal.<!EVALUATED("18446744073709551615")!>minus(twoVal)<!>
const val minus2 = twoVal.<!EVALUATED("0")!>minus(twoVal)<!>
const val minus3 = threeVal.<!EVALUATED("1")!>minus(twoVal)<!>
const val minus4 = twoVal.<!EVALUATED("0")!>minus(ubyteVal)<!>
const val minus5 = twoVal.<!EVALUATED("0")!>minus(ushortVal)<!>
const val minus6 = twoVal.<!EVALUATED("0")!>minus(uintVal)<!>

const val times1 = oneVal.<!EVALUATED("2")!>times(twoVal)<!>
const val times2 = twoVal.<!EVALUATED("4")!>times(twoVal)<!>
const val times3 = threeVal.<!EVALUATED("6")!>times(twoVal)<!>
const val times4 = twoVal.<!EVALUATED("4")!>times(ubyteVal)<!>
const val times5 = twoVal.<!EVALUATED("4")!>times(ushortVal)<!>
const val times6 = twoVal.<!EVALUATED("4")!>times(uintVal)<!>

const val div1 = oneVal.<!EVALUATED("0")!>div(twoVal)<!>
const val div2 = twoVal.<!EVALUATED("1")!>div(twoVal)<!>
const val div3 = threeVal.<!EVALUATED("1")!>div(twoVal)<!>
const val div4 = twoVal.<!EVALUATED("1")!>div(ubyteVal)<!>
const val div5 = twoVal.<!EVALUATED("1")!>div(ushortVal)<!>
const val div6 = twoVal.<!EVALUATED("1")!>div(uintVal)<!>

const val floorDiv1 = oneVal.<!EVALUATED("0")!>floorDiv(twoVal)<!>
const val floorDiv2 = twoVal.<!EVALUATED("1")!>floorDiv(twoVal)<!>
const val floorDiv3 = threeVal.<!EVALUATED("1")!>floorDiv(twoVal)<!>
const val floorDiv4 = twoVal.<!EVALUATED("1")!>floorDiv(ubyteVal)<!>
const val floorDiv5 = twoVal.<!EVALUATED("1")!>floorDiv(ushortVal)<!>
const val floorDiv6 = twoVal.<!EVALUATED("1")!>floorDiv(uintVal)<!>

const val rem1 = oneVal.<!EVALUATED("1")!>rem(twoVal)<!>
const val rem2 = twoVal.<!EVALUATED("0")!>rem(twoVal)<!>
const val rem3 = threeVal.<!EVALUATED("1")!>rem(twoVal)<!>
const val rem4 = twoVal.<!EVALUATED("0")!>rem(ubyteVal)<!>
const val rem5 = twoVal.<!EVALUATED("0")!>rem(ushortVal)<!>
const val rem6 = twoVal.<!EVALUATED("0")!>rem(uintVal)<!>

const val mod1 = oneVal.<!EVALUATED("1")!>mod(twoVal)<!>
const val mod2 = twoVal.<!EVALUATED("0")!>mod(twoVal)<!>
const val mod3 = threeVal.<!EVALUATED("1")!>mod(twoVal)<!>
const val mod4 = twoVal.<!EVALUATED("0")!>mod(ubyteVal)<!>
const val mod5 = twoVal.<!EVALUATED("0")!>mod(ushortVal)<!>
const val mod6 = twoVal.<!EVALUATED("0")!>mod(uintVal)<!>

const val and1 = oneVal.<!EVALUATED("0")!>and(twoVal)<!>
const val and2 = twoVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and3 = threeVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and4 = 12UL.<!EVALUATED("8")!>and(10UL)<!>

const val or1 = oneVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or2 = twoVal.<!EVALUATED("2")!>or(twoVal)<!>
const val or3 = threeVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or4 = 12UL.<!EVALUATED("14")!>or(10UL)<!>

const val xor1 = oneVal.<!EVALUATED("3")!>xor(twoVal)<!>
const val xor2 = twoVal.<!EVALUATED("0")!>xor(twoVal)<!>
const val xor3 = threeVal.<!EVALUATED("1")!>xor(twoVal)<!>
const val xor4 = 12UL.<!EVALUATED("6")!>xor(10UL)<!>

const val inv1 = zeroVal.<!EVALUATED("18446744073709551615")!>inv()<!>
const val inv2 = oneVal.<!EVALUATED("18446744073709551614")!>inv()<!>

const val shl1 = oneVal.<!EVALUATED("2")!>shl(1)<!>
const val shl2 = twoVal.<!EVALUATED("8")!>shl(2)<!>
const val shl3 = threeVal.<!EVALUATED("12")!>shl(2)<!>
const val shl4 = oneVal.<!EVALUATED("9223372036854775808")!>shl(63)<!>
const val shl5 = oneVal.<!EVALUATED("1")!>shl(64)<!>
const val shl6 = oneVal.<!EVALUATED("9223372036854775808")!>shl(127)<!>

const val shr1 = oneVal.<!EVALUATED("0")!>shr(1)<!>
const val shr2 = twoVal.<!EVALUATED("1")!>shr(1)<!>
const val shr3 = threeVal.<!EVALUATED("1")!>shr(1)<!>
const val shr4 = oneVal.<!EVALUATED("0")!>shr(63)<!>
const val shr5 = oneVal.<!EVALUATED("1")!>shr(64)<!>
const val shr6 = oneVal.<!EVALUATED("0")!>shr(127)<!>

const val convert1 = oneVal.<!EVALUATED("1")!>toUByte()<!>
const val convert2 = oneVal.<!EVALUATED("1")!>toUShort()<!>
const val convert3 = oneVal.<!EVALUATED("1")!>toUInt()<!>
const val convert4 = oneVal.<!EVALUATED("1")!>toULong()<!>
const val convert5 = oneVal.<!EVALUATED("1.0")!>toFloat()<!>
const val convert6 = oneVal.<!EVALUATED("1.0")!>toDouble()<!>
const val convert7 = oneVal.<!EVALUATED("1")!>toByte()<!>
const val convert8 = oneVal.<!EVALUATED("1")!>toShort()<!>
const val convert9 = oneVal.<!EVALUATED("1")!>toInt()<!>
const val convert10 = oneVal.<!EVALUATED("1")!>toLong()<!>
const val convert11 = 1.toByte().<!EVALUATED("1")!>toULong()<!>
const val convert12 = 1.toShort().<!EVALUATED("1")!>toULong()<!>
const val convert13 = 1.<!EVALUATED("1")!>toULong()<!>
const val convert14 = 1L.<!EVALUATED("1")!>toULong()<!>
const val convert15 = 1.0f.<!EVALUATED("1")!>toULong()<!>
const val convert16 = 1.0.<!EVALUATED("1")!>toULong()<!>

const val equals1 = <!EVALUATED("false")!>oneVal == twoVal<!>
const val equals2 = <!EVALUATED("true")!>twoVal == twoVal<!>
const val equals3 = <!EVALUATED("false")!>threeVal == twoVal<!>
const val equals4 = <!EVALUATED("false")!>fourVal == twoVal<!>

const val toString1 = oneVal.<!EVALUATED("1")!>toString()<!>
const val toString2 = twoVal.<!EVALUATED("2")!>toString()<!>

const val limits1 = <!EVALUATED("18446744073709551615")!>18446744073709551614UL+oneVal<!>
const val limits2 = <!EVALUATED("0")!>18446744073709551615UL+oneVal<!>
const val limits3 = <!EVALUATED("18446744073709551615")!>zeroVal-oneVal<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail compareTo1"
    if (compareTo2.id() != 0)    return "Fail compareTo2"
    if (compareTo3.id() != 1)    return "Fail compareTo3"
    if (compareTo4.id() != 0)    return "Fail compareTo4"
    if (compareTo5.id() != 0)    return "Fail compareTo5"
    if (compareTo6.id() != 0)    return "Fail compareTo6"

    if (plus1.id() != 3UL)     return "Fail plus1"
    if (plus2.id() != 4UL)     return "Fail plus2"
    if (plus3.id() != 5UL)     return "Fail plus3"
    if (plus4.id() != 4UL)     return "Fail plus4"
    if (plus5.id() != 4UL)     return "Fail plus5"
    if (plus6.id() != 4UL)     return "Fail plus6"

    if (minus1.id() != 18446744073709551615UL)      return "Fail minus1"
    if (minus2.id() != 0UL)                         return "Fail minus2"
    if (minus3.id() != 1UL)                         return "Fail minus3"
    if (minus4.id() != 0UL)                         return "Fail minus4"
    if (minus5.id() != 0UL)                         return "Fail minus5"
    if (minus6.id() != 0UL)                         return "Fail minus6"

    if (times1.id() != 2UL)        return "Fail times1"
    if (times2.id() != 4UL)        return "Fail times2"
    if (times3.id() != 6UL)        return "Fail times3"
    if (times4.id() != 4UL)        return "Fail times4"
    if (times5.id() != 4UL)        return "Fail times5"
    if (times6.id() != 4UL)        return "Fail times6"

    if (div1.id() != 0UL)          return "Fail div1"
    if (div2.id() != 1UL)          return "Fail div2"
    if (div3.id() != 1UL)          return "Fail div3"
    if (div4.id() != 1UL)          return "Fail div4"
    if (div5.id() != 1UL)          return "Fail div5"
    if (div6.id() != 1UL)          return "Fail div6"

    if (floorDiv1.id() != 0UL)          return "Fail  floorDiv1"
    if (floorDiv2.id() != 1UL)          return "Fail  floorDiv2"
    if (floorDiv3.id() != 1UL)          return "Fail  floorDiv3"
    if (floorDiv4.id() != 1UL)          return "Fail  floorDiv4"
    if (floorDiv5.id() != 1UL)          return "Fail  floorDiv5"
    if (floorDiv6.id() != 1UL)          return "Fail  floorDiv6"

    if (rem1.id() != 1UL)      return "Fail rem1"
    if (rem2.id() != 0UL)      return "Fail rem2"
    if (rem3.id() != 1UL)      return "Fail rem3"
    if (rem4.id() != 0UL)      return "Fail rem4"
    if (rem5.id() != 0UL)      return "Fail rem5"
    if (rem6.id() != 0UL)      return "Fail rem6"

    if (mod1.id() != 1UL)               return "Fail mod1"
    if (mod2.id() != 0UL)               return "Fail mod2"
    if (mod3.id() != 1UL)               return "Fail mod3"
    if (mod4.id() != 0u.toUByte())      return "Fail mod4"
    if (mod5.id() != 0u.toUShort())     return "Fail mod5"
    if (mod6.id() != 0u)                return "Fail mod6"

    if (and1.id() != 0UL)      return "Fail and1"
    if (and2.id() != 2UL)      return "Fail and2"
    if (and3.id() != 2UL)      return "Fail and3"
    if (and4.id() != 8UL)      return "Fail and4"

    if (or1.id() != 3UL)      return "Fail or1"
    if (or2.id() != 2UL)      return "Fail or2"
    if (or3.id() != 3UL)      return "Fail or3"
    if (or4.id() != 14UL)     return "Fail or4"

    if (xor1.id() != 3UL)      return "Fail xor1"
    if (xor2.id() != 0UL)      return "Fail xor2"
    if (xor3.id() != 1UL)      return "Fail xor3"
    if (xor4.id() != 6UL)      return "Fail xor4"

    if (inv1.id() != 18446744073709551615UL)      return "Fail inv1"
    if (inv2.id() != 18446744073709551614UL)      return "Fail inv2"

    if (shl1.id() != 2UL)                       return "Fail shl1"
    if (shl2.id() != 8UL)                       return "Fail shl2"
    if (shl3.id() != 12UL)                      return "Fail shl3"
    if (shl4.id() != 9223372036854775808UL)     return "Fail shl4"
    if (shl5.id() != 1UL)                       return "Fail shl5"
    if (shl6.id() != 9223372036854775808UL)     return "Fail shl6"

    if (shr1.id() != 0UL)                return "Fail shr1"
    if (shr2.id() != 1UL)                return "Fail shr2"
    if (shr3.id() != 1UL)                return "Fail shr3"
    if (shr4.id() != 0UL)                return "Fail shr4"
    if (shr5.id() != 1UL)                return "Fail shr5"
    if (shr6.id() != 0UL)                return "Fail shr6"

    if (convert1.id() != 1u.toUByte())   return "Fail convert1"
    if (convert2.id() != 1u.toUShort())  return "Fail convert2"
    if (convert3.id() != 1u)             return "Fail convert3"
    if (convert4.id() != 1UL)            return "Fail convert4"
    if (convert5.id() != 1.0f)           return "Fail convert5"
    if (convert6.id() != 1.0)            return "Fail convert6"
    if (convert7.id() != 1.toByte())     return "Fail convert7"
    if (convert8.id() != 1.toShort())    return "Fail convert8"
    if (convert9.id() != 1)              return "Fail convert9"
    if (convert10.id() != 1L)            return "Fail convert10"
    if (convert11.id() != 1UL)           return "Fail convert11"
    if (convert12.id() != 1UL)           return "Fail convert12"
    if (convert13.id() != 1UL)           return "Fail convert13"
    if (convert14.id() != 1UL)           return "Fail convert14"
    if (convert15.id() != 1UL)           return "Fail convert15"
    if (convert16.id() != 1UL)           return "Fail convert16"

    if (equals1.id() != false)   return "Fail equals1"
    if (equals2.id() != true)    return "Fail equals2"
    if (equals3.id() != false)   return "Fail equals3"
    if (equals4.id() != false)   return "Fail equals4"

    if (toString1.id() != "1")   return "Fail toString1"
    if (toString2.id() != "2")   return "Fail toString2"

    if (limits1.id() != 18446744073709551615UL)     return "Fail limits1"
    if (limits2.id() != 0UL)                        return "Fail limits2"
    if (limits3.id() != 18446744073709551615UL)     return "Fail limits3"

    return "OK"
}
