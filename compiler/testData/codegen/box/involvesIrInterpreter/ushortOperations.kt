// WITH_STDLIB
fun <T> T.id() = this

val compareTo1 = 1u.toUShort().compareTo(2u.toUShort())
val compareTo2 = 2u.toUShort().compareTo(2u.toUShort())
val compareTo3 = 3u.toUShort().compareTo(2u.toUShort())
val compareTo4 = 2u.toUShort().compareTo(2u.toUByte())
val compareTo5 = 2u.toUShort().compareTo(2u)
val compareTo6 = 2u.toUShort().compareTo(2UL)

val plus1 = 1u.toUShort().plus(2u.toUShort())
val plus2 = 2u.toUShort().plus(2u.toUShort())
val plus3 = 3u.toUShort().plus(2u.toUShort())
val plus4 = 2u.toUShort().plus(2u.toUByte())
val plus5 = 2u.toUShort().plus(2u)
val plus6 = 2u.toUShort().plus(2UL)

val minus1 = 2u.toUShort().minus(2u.toUShort())
val minus2 = 3u.toUShort().minus(2u.toUShort())
val minus3 = 2u.toUShort().minus(2u.toUByte())
val minus4 = 2u.toUShort().minus(2u)
val minus5 = 2u.toUShort().minus(2u.toULong())

val times1 = 1u.toUShort().times(2u.toUShort())
val times2 = 2u.toUShort().times(2u.toUShort())
val times3 = 3u.toUShort().times(2u.toUShort())
val times4 = 2u.toUShort().times(2u.toUByte())
val times5 = 2u.toUShort().times(2u)
val times6 = 2u.toUShort().times(2u.toULong())

val div1 = 1u.toUShort().div(2u.toUShort())
val div2 = 2u.toUShort().div(2u.toUShort())
val div3 = 3u.toUShort().div(2u.toUShort())
val div4 = 2u.toUShort().div(2u.toUByte())
val div5 = 2u.toUShort().div(2u)
val div6 = 2u.toUShort().div(2u.toULong())

val floorDiv1 = 1u.toUShort().floorDiv(2u.toUShort())
val floorDiv2 = 2u.toUShort().floorDiv(2u.toUShort())
val floorDiv3 = 3u.toUShort().floorDiv(2u.toUShort())
val floorDiv4 = 2u.toUShort().floorDiv(2u.toUByte())
val floorDiv5 = 2u.toUShort().floorDiv(2u)
val floorDiv6 = 2u.toUShort().floorDiv(2u.toULong())

val rem1 = 1u.toUShort().rem(2u.toUShort())
val rem2 = 2u.toUShort().rem(2u.toUShort())
val rem3 = 3u.toUShort().rem(2u.toUShort())
val rem4 = 2u.toUShort().rem(2u.toUByte())
val rem5 = 2u.toUShort().rem(2u)
val rem6 = 2u.toUShort().rem(2u.toULong())

val mod1 = 1u.toUShort().mod(2u.toUShort())
val mod2 = 2u.toUShort().mod(2u.toUShort())
val mod3 = 3u.toUShort().mod(2u.toUShort())
val mod4 = 2u.toUShort().mod(2u.toUByte())
val mod5 = 2u.toUShort().mod(2u)
val mod6 = 2u.toUShort().mod(2u.toULong())

val and1 = 1u.toUShort().and(1u.toUShort())
val and2 = 2u.toUShort().and(2u.toUShort())
val and3 = 3u.toUShort().and(2u.toUShort())
val and4 = 12u.toUShort().and(10u.toUShort())

val or1 = 1u.toUShort().or(1u.toUShort())
val or2 = 2u.toUShort().or(2u.toUShort())
val or3 = 3u.toUShort().or(2u.toUShort())
val or4 = 12u.toUShort().or(10u.toUShort())

val xor1 = 1u.toUShort().xor(1u.toUShort())
val xor2 = 2u.toUShort().xor(2u.toUShort())
val xor3 = 3u.toUShort().xor(2u.toUShort())
val xor4 = 12u.toUShort().xor(10u.toUShort())

val inv1 = 0u.toUShort().inv()
val inv2 = 1u.toUShort().inv()

val convert1 = 1u.toUShort().toUByte()
val convert2 = 1u.toUShort().toUShort()
val convert3 = 1u.toUShort().toUInt()
val convert4 = 1u.toUShort().toULong()
val convert5 = 1u.toUShort().toFloat()
val convert6 = 1u.toUShort().toDouble()
val convert7 = 1u.toUShort().toByte()
val convert8 = 1u.toUShort().toShort()
val convert9 = 1u.toUShort().toInt()
val convert10 = 1u.toUShort().toLong()

val equals1 = 1u.toUShort() == 2u.toUShort()
val equals2 = 2u.toUShort() == 2u.toUShort()
val equals3 = 3u.toUShort() == 2u.toUShort()
val equals4 = 4u.toUShort() == 2u.toUShort()

val toString1 = 1u.toUShort().toString()
val toString2 = 2u.toUShort().toString()

val limits1 = 65534u.toUShort()+1u.toUShort()
val limits2 = 65535u.toUShort()+1u.toUShort()
val limits3 = 0u.toUShort()-1u.toUShort()

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

    if (mod1.id() !=  1u.toUShort())    return "Fail mod1"
    if (mod2.id() !=  0u.toUShort())    return "Fail mod2"
    if (mod3.id() !=  1u.toUShort())    return "Fail mod3"
    if (mod4.id() != 0u.toUByte())      return "Fail mod4"
    if (mod5.id() != 0u)                return "Fail mod5"
    if (mod6.id() != 0UL)               return "Fail mod6"

    if (and1.id() !=  1u.toUShort())      return "Fail and1"
    if (and2.id() !=  2u.toUShort())      return "Fail and2"
    if (and3.id() !=  2u.toUShort())      return "Fail and3"
    if (and4.id() !=  8u.toUShort())      return "Fail and4"

    if (or1.id() !=  1u.toUShort())      return "Fail or1"
    if (or2.id() !=  2u.toUShort())      return "Fail or2"
    if (or3.id() !=  3u.toUShort())      return "Fail or3"
    if (or4.id() !=  14u.toUShort())     return "Fail or4"

    if (xor1.id() !=  0u.toUShort())      return "Fail xor1"
    if (xor2.id() !=  0u.toUShort())      return "Fail xor2"
    if (xor3.id() !=  1u.toUShort())      return "Fail xor3"
    if (xor4.id() !=  6u.toUShort())      return "Fail xor4"

    if (inv1.id() !=  65535u.toUShort())      return "Fail inv1"
    if (inv2.id() !=  65534u.toUShort())      return "Fail inv2"

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

    if (limits1.id() != 65535u)         return "Fail limits1"
    if (limits2.id() != 65536u)         return "Fail limits2"
    if (limits3.id() != 4294967295u)    return "Fail limits3"

    return "OK"
}
