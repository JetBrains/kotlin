// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ Ignore Backends that apply inliner before interpreter, which messes with the interpreter.
//     All Backends can be enabled once it's supported in the frontend.
// WITH_STDLIB
fun <T> T.id() = this

val compareTo1 = 1UL.<!EVALUATED{IR}("-1")!>compareTo(2UL)<!>
val compareTo2 = 2UL.<!EVALUATED{IR}("0")!>compareTo(2UL)<!>
val compareTo3 = 3UL.<!EVALUATED{IR}("1")!>compareTo(2UL)<!>
val compareTo4 = 2UL.<!EVALUATED{IR}("0")!>compareTo(2u.toUByte())<!>
val compareTo5 = 2UL.<!EVALUATED{IR}("0")!>compareTo(2u.toUShort())<!>
val compareTo6 = 2UL.<!EVALUATED{IR}("0")!>compareTo(2u)<!>

val plus1 = 1UL.<!EVALUATED{IR}("3")!>plus(2UL)<!>
val plus2 = 2UL.<!EVALUATED{IR}("4")!>plus(2UL)<!>
val plus3 = 3UL.<!EVALUATED{IR}("5")!>plus(2UL)<!>
val plus4 = 2UL.<!EVALUATED{IR}("4")!>plus(2u.toUByte())<!>
val plus5 = 2UL.<!EVALUATED{IR}("4")!>plus(2u.toUShort())<!>
val plus6 = 2UL.<!EVALUATED{IR}("4")!>plus(2u)<!>

val minus1 = 1UL.<!EVALUATED{IR}("18446744073709551615")!>minus(2UL)<!>
val minus2 = 2UL.<!EVALUATED{IR}("0")!>minus(2UL)<!>
val minus3 = 3UL.<!EVALUATED{IR}("1")!>minus(2UL)<!>
val minus4 = 2UL.<!EVALUATED{IR}("0")!>minus(2u.toUByte())<!>
val minus5 = 2UL.<!EVALUATED{IR}("0")!>minus(2u.toUShort())<!>
val minus6 = 2UL.<!EVALUATED{IR}("0")!>minus(2u)<!>

val times1 = 1UL.<!EVALUATED{IR}("2")!>times(2UL)<!>
val times2 = 2UL.<!EVALUATED{IR}("4")!>times(2UL)<!>
val times3 = 3UL.<!EVALUATED{IR}("6")!>times(2UL)<!>
val times4 = 2UL.<!EVALUATED{IR}("4")!>times(2u.toUByte())<!>
val times5 = 2UL.<!EVALUATED{IR}("4")!>times(2u.toUShort())<!>
val times6 = 2UL.<!EVALUATED{IR}("4")!>times(2u)<!>

val div1 = 1UL.<!EVALUATED{IR}("0")!>div(2UL)<!>
val div2 = 2UL.<!EVALUATED{IR}("1")!>div(2UL)<!>
val div3 = 3UL.<!EVALUATED{IR}("1")!>div(2UL)<!>
val div4 = 2UL.<!EVALUATED{IR}("1")!>div(2u.toUByte())<!>
val div5 = 2UL.<!EVALUATED{IR}("1")!>div(2u.toUShort())<!>
val div6 = 2UL.<!EVALUATED{IR}("1")!>div(2u)<!>

val floorDiv1 = 1UL.<!EVALUATED{IR}("0")!>floorDiv(2UL)<!>
val floorDiv2 = 2UL.<!EVALUATED{IR}("1")!>floorDiv(2UL)<!>
val floorDiv3 = 3UL.<!EVALUATED{IR}("1")!>floorDiv(2UL)<!>
val floorDiv4 = 2UL.<!EVALUATED{IR}("1")!>floorDiv(2u.toUByte())<!>
val floorDiv5 = 2UL.<!EVALUATED{IR}("1")!>floorDiv(2u.toUShort())<!>
val floorDiv6 = 2UL.<!EVALUATED{IR}("1")!>floorDiv(2u)<!>

val rem1 = 1UL.<!EVALUATED{IR}("1")!>rem(2UL)<!>
val rem2 = 2UL.<!EVALUATED{IR}("0")!>rem(2UL)<!>
val rem3 = 3UL.<!EVALUATED{IR}("1")!>rem(2UL)<!>
val rem4 = 2UL.<!EVALUATED{IR}("0")!>rem(2u.toUByte())<!>
val rem5 = 2UL.<!EVALUATED{IR}("0")!>rem(2u.toUShort())<!>
val rem6 = 2UL.<!EVALUATED{IR}("0")!>rem(2u)<!>

val mod1 = 1UL.<!EVALUATED{IR}("1")!>mod(2UL)<!>
val mod2 = 2UL.<!EVALUATED{IR}("0")!>mod(2UL)<!>
val mod3 = 3UL.<!EVALUATED{IR}("1")!>mod(2UL)<!>
val mod4 = 2UL.<!EVALUATED{IR}("0")!>mod(2u.toUByte())<!>
val mod5 = 2UL.<!EVALUATED{IR}("0")!>mod(2u.toUShort())<!>
val mod6 = 2UL.<!EVALUATED{IR}("0")!>mod(2UL)<!>

val and1 = 1UL.<!EVALUATED{IR}("0")!>and(2UL)<!>
val and2 = 2UL.<!EVALUATED{IR}("2")!>and(2UL)<!>
val and3 = 3UL.<!EVALUATED{IR}("2")!>and(2UL)<!>
val and4 = 12UL.<!EVALUATED{IR}("8")!>and(10UL)<!>

val or1 = 1UL.<!EVALUATED{IR}("3")!>or(2UL)<!>
val or2 = 2UL.<!EVALUATED{IR}("2")!>or(2UL)<!>
val or3 = 3UL.<!EVALUATED{IR}("3")!>or(2UL)<!>
val or4 = 12UL.<!EVALUATED{IR}("14")!>or(10UL)<!>

val xor1 = 1UL.<!EVALUATED{IR}("3")!>xor(2UL)<!>
val xor2 = 2UL.<!EVALUATED{IR}("0")!>xor(2UL)<!>
val xor3 = 3UL.<!EVALUATED{IR}("1")!>xor(2UL)<!>
val xor4 = 12UL.<!EVALUATED{IR}("6")!>xor(10UL)<!>

val inv1 = 0UL.<!EVALUATED{IR}("18446744073709551615")!>inv()<!>
val inv2 = 1UL.<!EVALUATED{IR}("18446744073709551614")!>inv()<!>

val shl1 = 1UL.<!EVALUATED{IR}("2")!>shl(1)<!>
val shl2 = 2UL.<!EVALUATED{IR}("8")!>shl(2)<!>
val shl3 = 3UL.<!EVALUATED{IR}("12")!>shl(2)<!>
val shl4 = 1UL.<!EVALUATED{IR}("9223372036854775808")!>shl(63)<!>
val shl5 = 1UL.<!EVALUATED{IR}("1")!>shl(64)<!>
val shl6 = 1UL.<!EVALUATED{IR}("9223372036854775808")!>shl(127)<!>

val shr1 = 1UL.<!EVALUATED{IR}("0")!>shr(1)<!>
val shr2 = 2UL.<!EVALUATED{IR}("1")!>shr(1)<!>
val shr3 = 3UL.<!EVALUATED{IR}("1")!>shr(1)<!>
val shr4 = 1UL.<!EVALUATED{IR}("0")!>shr(63)<!>
val shr5 = 1UL.<!EVALUATED{IR}("1")!>shr(64)<!>
val shr6 = 1UL.<!EVALUATED{IR}("0")!>shr(127)<!>

val convert1 = 1UL.<!EVALUATED{IR}("1")!>toUByte()<!>
val convert2 = 1UL.<!EVALUATED{IR}("1")!>toUShort()<!>
val convert3 = 1UL.<!EVALUATED{IR}("1")!>toUInt()<!>
val convert4 = 1UL.<!EVALUATED{IR}("1")!>toULong()<!>
val convert5 = 1UL.<!EVALUATED{IR}("1.0")!>toFloat()<!>
val convert6 = 1UL.<!EVALUATED{IR}("1.0")!>toDouble()<!>
val convert7 = 1UL.<!EVALUATED{IR}("1")!>toByte()<!>
val convert8 = 1UL.<!EVALUATED{IR}("1")!>toShort()<!>
val convert9 = 1UL.<!EVALUATED{IR}("1")!>toInt()<!>
val convert10 = 1UL.<!EVALUATED{IR}("1")!>toLong()<!>
val convert11 = 1.<!EVALUATED{IR}("1")!>toByte()<!>.toULong()
val convert12 = 1.<!EVALUATED{IR}("1")!>toShort()<!>.toULong()
val convert13 = 1.toULong()
val convert14 = 1L.toULong()
val convert15 = 1.0f.toULong()
val convert16 = 1.0.toULong()

val equals1 = <!EVALUATED{IR}("false")!>1UL == 2UL<!>
val equals2 = <!EVALUATED{IR}("true")!>2UL == 2UL<!>
val equals3 = <!EVALUATED{IR}("false")!>3UL == 2UL<!>
val equals4 = <!EVALUATED{IR}("false")!>4UL == 2UL<!>

val toString1 = 1UL.<!EVALUATED{IR}("1")!>toString()<!>
val toString2 = 2UL.<!EVALUATED{IR}("2")!>toString()<!>

val limits1 = <!EVALUATED{IR}("18446744073709551615")!>18446744073709551614UL+1UL<!>
val limits2 = <!EVALUATED{IR}("0")!>18446744073709551615UL+1UL<!>
val limits3 = <!EVALUATED{IR}("18446744073709551615")!>0UL-1UL<!>

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
    if (mod6.id() != 0UL)               return "Fail mod6"

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
    if (shl6.id() != 9223372036854775808UL)      return "Fail shl6"

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
