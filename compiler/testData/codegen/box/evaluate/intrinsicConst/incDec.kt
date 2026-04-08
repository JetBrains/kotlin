// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB
fun <T> T.id() = this

const val incByte = 1.toByte().inc()
const val decByte = 1.toByte().dec()
const val incMaxByte = Byte.MAX_VALUE.inc()
const val decMinByte = Byte.MIN_VALUE.dec()
const val incDecChainedByte = 1.toByte().inc().dec().inc().dec()

const val incShort = 1.toShort().inc()
const val decShort = 1.toShort().dec()
const val incMaxShort = Short.MAX_VALUE.inc()
const val decMinShort = Short.MIN_VALUE.dec()
const val incDecChainedShort = 1.toShort().inc().dec().inc().dec()

const val incInt = 1.inc()
const val decInt = 1.dec()
const val incMaxInt = Int.MAX_VALUE.inc()
const val decMinInt = Int.MIN_VALUE.dec()
const val incDecChainedInt = 1.inc().dec().inc().dec()

const val incLong = 1L.inc()
const val decLong = 1L.dec()
const val incMaxLong = Long.MAX_VALUE.inc()
const val decMinLong = Long.MIN_VALUE.dec()
const val incDecChainedLong = 1L.inc().dec().inc().dec()

const val incUByte = 1u.toUByte().inc()
const val decUByte = 1u.toUByte().dec()
const val incMaxUByte = UByte.MAX_VALUE.inc()
const val decMinUByte = UByte.MIN_VALUE.dec()
const val incDecChainedUByte = 1u.toUByte().inc().dec().inc().dec()

const val incUShort = 1u.toUShort().inc()
const val decUShort = 1u.toUShort().dec()
const val incMaxUShort = UShort.MAX_VALUE.inc()
const val decMinUShort = UShort.MIN_VALUE.dec()
const val incDecChainedUShort = 1u.toUShort().inc().dec().inc().dec()

const val incUInt = 1u.inc()
const val decUInt = 1u.dec()
const val incMaxUInt = UInt.MAX_VALUE.inc()
const val decMinUInt = UInt.MIN_VALUE.dec()
const val incDecChainedUInt = 1u.inc().dec().inc().dec()

const val incULong = 1uL.inc()
const val decULong = 1uL.dec()
const val incMaxULong = ULong.MAX_VALUE.inc()
const val decMinULong = ULong.MIN_VALUE.dec()
const val incDecChainedULong = 1uL.inc().dec().inc().dec()

fun box(): String {
    if (incByte.id() != 2.toByte())                 return "Fail incByte"
    if (decByte.id() != 0.toByte())                 return "Fail decByte"
    if (incMaxByte.id() != Byte.MIN_VALUE)          return "Fail incMaxByte"
    if (decMinByte.id() != Byte.MAX_VALUE)          return "Fail decMinByte"
    if (incDecChainedByte.id() != 1.toByte())       return "Fail incDecChainedByte"

    if (incShort.id() != 2.toShort())               return "Fail incShort"
    if (decShort.id() != 0.toShort())               return "Fail decShort"
    if (incMaxShort.id() != Short.MIN_VALUE)        return "Fail incMaxShort"
    if (decMinShort.id() != Short.MAX_VALUE)        return "Fail decMinShort"
    if (incDecChainedShort.id() != 1.toShort())     return "Fail incDecChainedShort"

    if (incInt.id() != 2)                           return "Fail incInt"
    if (decInt.id() != 0)                           return "Fail decInt"
    if (incMaxInt.id() != Int.MIN_VALUE)            return "Fail incMaxInt"
    if (decMinInt.id() != Int.MAX_VALUE)            return "Fail decMinInt"
    if (incDecChainedInt.id() != 1)                 return "Fail incDecChainedInt"

    if (incLong.id() != 2L)                         return "Fail incLong"
    if (decLong.id() != 0L)                         return "Fail decLong"
    if (incMaxLong.id() != Long.MIN_VALUE)          return "Fail incMaxLong"
    if (decMinLong.id() != Long.MAX_VALUE)          return "Fail decMinLong"
    if (incDecChainedLong.id() != 1L)               return "Fail incDecChainedLong"

    if (incUByte.id() != 2u.toUByte())              return "Fail incUByte"
    if (decUByte.id() != 0u.toUByte())              return "Fail decUByte"
    if (incMaxUByte.id() != UByte.MIN_VALUE)        return "Fail incMaxUByte"
    if (decMinUByte.id() != UByte.MAX_VALUE)        return "Fail decMinUByte"
    if (incDecChainedUByte.id() != 1u.toUByte())    return "Fail incDecChainedUByte"

    if (incUShort.id() != 2u.toUShort())            return "Fail incUShort"
    if (decUShort.id() != 0u.toUShort())            return "Fail decUShort"
    if (incMaxUShort.id() != UShort.MIN_VALUE)      return "Fail incMaxUShort"
    if (decMinUShort.id() != UShort.MAX_VALUE)      return "Fail decMinUShort"
    if (incDecChainedUShort.id() != 1u.toUShort())  return "Fail incDecChainedUShort"

    if (incUInt.id() != 2u)                         return "Fail incUInt"
    if (decUInt.id() != 0u)                         return "Fail decUInt"
    if (incMaxUInt.id() != UInt.MIN_VALUE)          return "Fail incMaxUInt"
    if (decMinUInt.id() != UInt.MAX_VALUE)          return "Fail decMinUInt"
    if (incDecChainedUInt.id() != 1u)               return "Fail incDecChainedUInt"

    if (incULong.id() != 2uL)                       return "Fail incULong"
    if (decULong.id() != 0uL)                       return "Fail decULong"
    if (incMaxULong.id() != ULong.MIN_VALUE)        return "Fail incMaxULong"
    if (decMinULong.id() != ULong.MAX_VALUE)        return "Fail decMinULong"
    if (incDecChainedULong.id() != 1uL)             return "Fail incDecChainedULong"

    return "OK"
}
