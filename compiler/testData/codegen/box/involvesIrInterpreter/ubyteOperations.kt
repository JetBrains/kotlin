// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: ANY
// ^^^^^^^^^^^^^^^^^^^^^^ Unsigned operations will not be supported in the legacy K1 frontend.
// WITH_STDLIB
fun <T> T.id() = this

const val zeroVal = 0u.<!EVALUATED("0")!>toUByte()<!>
const val oneVal = 1u.<!EVALUATED("1")!>toUByte()<!>
const val twoVal = 2u.<!EVALUATED("2")!>toUByte()<!>
const val threeVal = 3u.<!EVALUATED("3")!>toUByte()<!>
const val fourVal = 4u.<!EVALUATED("4")!>toUByte()<!>

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
const val compareTo4 = twoVal.<!EVALUATED("0")!>compareTo(ushortVal)<!>
const val compareTo5 = twoVal.<!EVALUATED("0")!>compareTo(uintVal)<!>
const val compareTo6 = twoVal.<!EVALUATED("0")!>compareTo(ulongVal)<!>

const val plus1 = oneVal.<!EVALUATED("3")!>plus(twoVal)<!>
const val plus2 = twoVal.<!EVALUATED("4")!>plus(twoVal)<!>
const val plus3 = threeVal.<!EVALUATED("5")!>plus(twoVal)<!>
const val plus4 = twoVal.<!EVALUATED("4")!>plus(ushortVal)<!>
const val plus5 = twoVal.<!EVALUATED("4")!>plus(uintVal)<!>
const val plus6 = twoVal.<!EVALUATED("4")!>plus(ulongVal)<!>

const val minus1 = oneVal.<!EVALUATED("4294967295")!>minus(twoVal)<!>
const val minus2 = twoVal.<!EVALUATED("0")!>minus(twoVal)<!>
const val minus3 = threeVal.<!EVALUATED("1")!>minus(twoVal)<!>
const val minus4 = twoVal.<!EVALUATED("0")!>minus(ushortVal)<!>
const val minus5 = twoVal.<!EVALUATED("0")!>minus(uintVal)<!>
const val minus6 = twoVal.<!EVALUATED("0")!>minus(ulongVal)<!>

const val times1 = oneVal.<!EVALUATED("2")!>times(twoVal)<!>
const val times2 = twoVal.<!EVALUATED("4")!>times(twoVal)<!>
const val times3 = threeVal.<!EVALUATED("6")!>times(twoVal)<!>
const val times4 = twoVal.<!EVALUATED("4")!>times(ushortVal)<!>
const val times5 = twoVal.<!EVALUATED("4")!>times(uintVal)<!>
const val times6 = twoVal.<!EVALUATED("4")!>times(ulongVal)<!>

const val div1 = oneVal.<!EVALUATED("0")!>div(twoVal)<!>
const val div2 = twoVal.<!EVALUATED("1")!>div(twoVal)<!>
const val div3 = threeVal.<!EVALUATED("1")!>div(twoVal)<!>
const val div4 = twoVal.<!EVALUATED("1")!>div(ushortVal)<!>
const val div5 = twoVal.<!EVALUATED("1")!>div(uintVal)<!>
const val div6 = twoVal.<!EVALUATED("1")!>div(ulongVal)<!>

const val floorDiv1 = oneVal.<!EVALUATED("0")!>floorDiv(twoVal)<!>
const val floorDiv2 = twoVal.<!EVALUATED("1")!>floorDiv(twoVal)<!>
const val floorDiv3 = threeVal.<!EVALUATED("1")!>floorDiv(twoVal)<!>
const val floorDiv4 = twoVal.<!EVALUATED("1")!>floorDiv(ushortVal)<!>
const val floorDiv5 = twoVal.<!EVALUATED("1")!>floorDiv(uintVal)<!>
const val floorDiv6 = twoVal.<!EVALUATED("1")!>floorDiv(ulongVal)<!>

const val rem1 = oneVal.<!EVALUATED("1")!>rem(twoVal)<!>
const val rem2 = twoVal.<!EVALUATED("0")!>rem(twoVal)<!>
const val rem3 = threeVal.<!EVALUATED("1")!>rem(twoVal)<!>
const val rem4 = twoVal.<!EVALUATED("0")!>rem(ushortVal)<!>
const val rem5 = twoVal.<!EVALUATED("0")!>rem(uintVal)<!>
const val rem6 = twoVal.<!EVALUATED("0")!>rem(ulongVal)<!>

const val mod1 = oneVal.<!EVALUATED("1")!>mod(twoVal)<!>
const val mod2 = twoVal.<!EVALUATED("0")!>mod(twoVal)<!>
const val mod3 = threeVal.<!EVALUATED("1")!>mod(twoVal)<!>
const val mod4 = twoVal.<!EVALUATED("0")!>mod(ushortVal)<!>
const val mod5 = twoVal.<!EVALUATED("0")!>mod(uintVal)<!>
const val mod6 = twoVal.<!EVALUATED("0")!>mod(ulongVal)<!>

const val and1 = oneVal.<!EVALUATED("0")!>and(twoVal)<!>
const val and2 = twoVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and3 = threeVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and4 = 12u.toUByte().<!EVALUATED("8")!>and(10u.toUByte())<!>

const val or1 = oneVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or2 = twoVal.<!EVALUATED("2")!>or(twoVal)<!>
const val or3 = threeVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or4 = 12u.toUByte().<!EVALUATED("14")!>or(10u.toUByte())<!>

const val xor1 = oneVal.<!EVALUATED("3")!>xor(twoVal)<!>
const val xor2 = twoVal.<!EVALUATED("0")!>xor(twoVal)<!>
const val xor3 = threeVal.<!EVALUATED("1")!>xor(twoVal)<!>
const val xor4 = 12u.toUByte().<!EVALUATED("6")!>xor(10u.toUByte())<!>

const val inv1 = zeroVal.<!EVALUATED("255")!>inv()<!>
const val inv2 = oneVal.<!EVALUATED("254")!>inv()<!>

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
const val convert11 = 1.toByte().<!EVALUATED("1")!>toUByte()<!>
const val convert12 = 1.toShort().<!EVALUATED("1")!>toUByte()<!>
const val convert13 = 1.<!EVALUATED("1")!>toUByte()<!>
const val convert14 = 1L.<!EVALUATED("1")!>toUByte()<!>

const val equals1 = <!EVALUATED("false")!>oneVal == twoVal<!>
const val equals2 = <!EVALUATED("true")!>twoVal == twoVal<!>
const val equals3 = <!EVALUATED("false")!>threeVal == twoVal<!>
const val equals4 = <!EVALUATED("false")!>fourVal == twoVal<!>

const val toString1 = oneVal.<!EVALUATED("1")!>toString()<!>
const val toString2 = twoVal.<!EVALUATED("2")!>toString()<!>

const val limits1 = <!EVALUATED("255")!>254u.toUByte()+oneVal<!>
const val limits2 = <!EVALUATED("256")!>255u.toUByte()+oneVal<!>
const val limits3 = <!EVALUATED("4294967295")!>zeroVal-oneVal<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail compareTo1"
    if (compareTo2.id() != 0)    return "Fail compareTo2"
    if (compareTo3.id() != 1)    return "Fail compareTo3"
    if (compareTo4.id() != 0)    return "Fail compareTo4"
    if (compareTo5.id() != 0)    return "Fail compareTo5"
    if (compareTo6.id() != 0)    return "Fail compareTo6"

    if (plus1.id() != 3u)    return "Fail plus1"
    if (plus2.id() != 4u)    return "Fail plus2"
    if (plus3.id() != 5u)    return "Fail plus3"
    if (plus4.id() != 4u)    return "Fail plus4"
    if (plus5.id() != 4u)    return "Fail plus5"
    if (plus6.id() != 4UL)   return "Fail plus6"

    if (minus1.id() != 4294967295u)     return "Fail minus1"
    if (minus2.id() != 0u)              return "Fail minus2"
    if (minus3.id() != 1u)              return "Fail minus3"
    if (minus4.id() != 0u)              return "Fail minus4"
    if (minus5.id() != 0u)              return "Fail minus5"
    if (minus6.id() != 0UL)             return "Fail minus6"

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

    if ( floorDiv1.id() != 0u)          return "Fail  floorDiv1"
    if ( floorDiv2.id() != 1u)          return "Fail  floorDiv2"
    if ( floorDiv3.id() != 1u)          return "Fail  floorDiv3"
    if ( floorDiv4.id() != 1u)          return "Fail  floorDiv4"
    if ( floorDiv5.id() != 1u)          return "Fail  floorDiv5"
    if ( floorDiv6.id() != 1uL)         return "Fail  floorDiv6"

    if (rem1.id() != 1u)      return "Fail rem1"
    if (rem2.id() != 0u)      return "Fail rem2"
    if (rem3.id() != 1u)      return "Fail rem3"
    if (rem4.id() != 0u)      return "Fail rem4"
    if (rem5.id() != 0u)      return "Fail rem5"
    if (rem6.id() != 0UL)     return "Fail rem6"

    if (mod1.id() !=  1u.toUByte())     return "Fail mod1"
    if (mod2.id() !=  0u.toUByte())     return "Fail mod2"
    if (mod3.id() !=  1u.toUByte())     return "Fail mod3"
    if (mod4.id() != 0u.toUShort())     return "Fail mod4"
    if (mod5.id() != 0u)                return "Fail mod5"
    if (mod6.id() != 0UL)               return "Fail mod6"

    if (and1.id() !=  0u.toUByte())      return "Fail and1"
    if (and2.id() !=  2u.toUByte())      return "Fail and2"
    if (and3.id() !=  2u.toUByte())      return "Fail and3"
    if (and4.id() !=  8u.toUByte())      return "Fail and4"

    if (or1.id() !=  3u.toUByte())      return "Fail or1"
    if (or2.id() !=  2u.toUByte())      return "Fail or2"
    if (or3.id() !=  3u.toUByte())      return "Fail or3"
    if (or4.id() !=  14u.toUByte())     return "Fail or4"

    if (xor1.id() !=  3u.toUByte())      return "Fail xor1"
    if (xor2.id() !=  0u.toUByte())      return "Fail xor2"
    if (xor3.id() !=  1u.toUByte())      return "Fail xor3"
    if (xor4.id() !=  6u.toUByte())      return "Fail xor4"

    if (inv1.id() !=  65535u.toUByte())      return "Fail inv1"
    if (inv2.id() !=  65534u.toUByte())      return "Fail inv2"

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
    if (convert11.id() != 1u.toUByte())  return "Fail convert11"
    if (convert12.id() != 1u.toUByte())  return "Fail convert12"
    if (convert13.id() != 1u.toUByte())  return "Fail convert13"
    if (convert14.id() != 1u.toUByte())  return "Fail convert14"

    if (equals1.id() != false)   return "Fail equals1"
    if (equals2.id() != true)    return "Fail equals2"
    if (equals3.id() != false)   return "Fail equals3"
    if (equals4.id() != false)   return "Fail equals4"

    if (toString1.id() != "1")   return "Fail toString1"
    if (toString2.id() != "2")   return "Fail toString2"

    if (limits1.id() != 255u)           return "Fail limits1"
    if (limits2.id() != 256u)           return "Fail limits2"
    if (limits3.id() != 4294967295u)    return "Fail limits3"

    return "OK"
}
