// WITH_STDLIB
fun <T> T.id() = this

val compareTo1 = 1UL.compareTo(2UL)
val compareTo2 = 2UL.compareTo(2UL)
val compareTo3 = 3UL.compareTo(2UL)
val compareTo4 = 2UL.compareTo(2u.toUByte())
val compareTo5 = 2UL.compareTo(2u.toUShort())
val compareTo6 = 2UL.compareTo(2u)

val plus1 = 1UL.plus(2UL)
val plus2 = 2UL.plus(2UL)
val plus3 = 3UL.plus(2UL)
val plus4 = 2UL.plus(2u.toUByte())
val plus5 = 2UL.plus(2u.toUShort())
val plus6 = 2UL.plus(2u)

val minus1 = 2UL.minus(2UL)
val minus2 = 3UL.minus(2UL)
val minus3 = 2UL.minus(2u.toUByte())
val minus4 = 2UL.minus(2u.toUShort())
val minus5 = 2UL.minus(2u)

val times1 = 1UL.times(2UL)
val times2 = 2UL.times(2UL)
val times3 = 3UL.times(2UL)
val times4 = 2UL.times(2u.toUByte())
val times5 = 2UL.times(2u.toUShort())
val times6 = 2UL.times(2u)

val div1 = 1UL.div(2UL)
val div2 = 2UL.div(2UL)
val div3 = 3UL.div(2UL)
val div4 = 2UL.div(2u.toUByte())
val div5 = 2UL.div(2u.toUShort())
val div6 = 2UL.div(2u)

val floorDiv1 = 1UL.floorDiv(2UL)
val floorDiv2 = 2UL.floorDiv(2UL)
val floorDiv3 = 3UL.floorDiv(2UL)
val floorDiv4 = 2UL.floorDiv(2u.toUByte())
val floorDiv5 = 2UL.floorDiv(2u.toUShort())
val floorDiv6 = 2UL.floorDiv(2u)

val rem1 = 1UL.rem(2UL)
val rem2 = 2UL.rem(2UL)
val rem3 = 3UL.rem(2UL)
val rem4 = 2UL.rem(2u.toUByte())
val rem5 = 2UL.rem(2u.toUShort())
val rem6 = 2UL.rem(2u)

val mod1 = 1UL.mod(2UL)
val mod2 = 2UL.mod(2UL)
val mod3 = 3UL.mod(2UL)
val mod4 = 2UL.mod(2u.toUByte())
val mod5 = 2UL.mod(2u.toUShort())
val mod6 = 2UL.mod(2u.toULong())

val and1 = 1UL.and(1UL)
val and2 = 2UL.and(2UL)
val and3 = 3UL.and(2UL)
val and4 = 12UL.and(10UL)

val or1 = 1UL.or(1UL)
val or2 = 2UL.or(2UL)
val or3 = 3UL.or(2UL)
val or4 = 12UL.or(10UL)

val xor1 = 1UL.xor(1UL)
val xor2 = 2UL.xor(2UL)
val xor3 = 3UL.xor(2UL)
val xor4 = 12UL.xor(10UL)

val inv1 = 0UL.inv()
val inv2 = 1UL.inv()

val shl1 = 1UL.shl(1)
val shl2 = 2UL.shl(2)
val shl3 = 3UL.shl(2)
val shl4 = 1UL.shl(63)
val shl5 = 1UL.shl(64)
val shl6 = 1UL.shl(127)

val shr1 = 1UL.shr(1)
val shr2 = 2UL.shr(1)
val shr3 = 3UL.shr(1)
val shr4 = 1UL.shr(63)
val shr5 = 1UL.shr(64)
val shr6 = 1UL.shr(127)

val convert1 = 1UL.toUByte()
val convert2 = 1UL.toUShort()
val convert3 = 1UL.toUInt()
val convert4 = 1UL.toULong()
val convert5 = 1UL.toFloat()
val convert6 = 1UL.toDouble()
val convert7 = 1UL.toByte()
val convert8 = 1UL.toShort()
val convert9 = 1UL.toInt()
val convert10 = 1UL.toLong()

val equals1 = 1UL == 2UL
val equals2 = 2UL == 2UL
val equals3 = 3UL == 2UL
val equals4 = 4UL == 2UL

val toString1 = 1UL.toString()
val toString2 = 2UL.toString()

val limits1 = 18446744073709551614UL+1UL
val limits2 = 18446744073709551615UL+1UL
val limits3 = 0UL-1UL

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

    if (minus1.id() != 0UL)        return "Fail minus1"
    if (minus2.id() != 1UL)        return "Fail minus2"
    if (minus3.id() != 0UL)        return "Fail minus3"
    if (minus4.id() != 0UL)        return "Fail minus4"
    if (minus5.id() != 0UL)        return "Fail minus5"

    if (times1.id() != 2UL)        return "Fail times1"
    if (times2.id() != 4UL)        return "Fail times2"
    if (times3.id() != 6UL)        return "Fail times3"
    if (times4.id() != 4UL)        return "Fail times4"
    if (times5.id() != 4UL)        return "Fail times5"
    if (times6.id() != 4UL)       return "Fail times6"

    if (div1.id() != 0UL)          return "Fail div1"
    if (div2.id() != 1UL)          return "Fail div2"
    if (div3.id() != 1UL)          return "Fail div3"
    if (div4.id() != 1UL)          return "Fail div4"
    if (div5.id() != 1UL)          return "Fail div5"
    if (div6.id() != 1uL)         return "Fail div6"

    if (rem1.id() != 1UL)      return "Fail rem1"
    if (rem2.id() != 0UL)      return "Fail rem2"
    if (rem3.id() != 1UL)      return "Fail rem3"
    if (rem4.id() != 0UL)      return "Fail rem4"
    if (rem5.id() != 0UL)      return "Fail rem5"
    if (rem6.id() != 0UL)     return "Fail rem6"

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

    if (limits1.id() != 18446744073709551615UL)     return "Fail limits1"
    if (limits2.id() != 0UL)                        return "Fail limits2"
    if (limits3.id() != 18446744073709551615UL)     return "Fail limits3"

    return "OK"
}
