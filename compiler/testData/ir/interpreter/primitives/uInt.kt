import kotlin.*

@CompileTimeCalculation fun compareTo(value: UInt, other: UByte) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: UInt, other: UShort) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: UInt, other: UInt) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: UInt, other: ULong) = value.compareTo(other)

@CompileTimeCalculation fun plus(value: UInt, other: UByte) = value.plus(other)
@CompileTimeCalculation fun plus(value: UInt, other: UShort) = value.plus(other)
@CompileTimeCalculation fun plus(value: UInt, other: UInt) = value.plus(other)
@CompileTimeCalculation fun plus(value: UInt, other: ULong) = value.plus(other)

@CompileTimeCalculation fun minus(value: UInt, other: UByte) = value.minus(other)
@CompileTimeCalculation fun minus(value: UInt, other: UShort) = value.minus(other)
@CompileTimeCalculation fun minus(value: UInt, other: UInt) = value.minus(other)
@CompileTimeCalculation fun minus(value: UInt, other: ULong) = value.minus(other)

@CompileTimeCalculation fun times(value: UInt, other: UByte) = value.times(other)
@CompileTimeCalculation fun times(value: UInt, other: UShort) = value.times(other)
@CompileTimeCalculation fun times(value: UInt, other: UInt) = value.times(other)
@CompileTimeCalculation fun times(value: UInt, other: ULong) = value.times(other)

@CompileTimeCalculation fun div(value: UInt, other: UByte) = value.div(other)
@CompileTimeCalculation fun div(value: UInt, other: UShort) = value.div(other)
@CompileTimeCalculation fun div(value: UInt, other: UInt) = value.div(other)
@CompileTimeCalculation fun div(value: UInt, other: ULong) = value.div(other)

@CompileTimeCalculation fun rem(value: UInt, other: UByte) = value.rem(other)
@CompileTimeCalculation fun rem(value: UInt, other: UShort) = value.rem(other)
@CompileTimeCalculation fun rem(value: UInt, other: UInt) = value.rem(other)
@CompileTimeCalculation fun rem(value: UInt, other: ULong) = value.rem(other)

@CompileTimeCalculation fun inc(value: UInt) = value.inc()
@CompileTimeCalculation fun dec(value: UInt) = value.dec()

@CompileTimeCalculation fun rangeTo(value: UInt, other: UInt) = value.rangeTo(other)

@CompileTimeCalculation fun shl(value: UInt, bitCount: Int) = value.shl(bitCount)
@CompileTimeCalculation fun shr(value: UInt, bitCount: Int) = value.shr(bitCount)
@CompileTimeCalculation fun and(value: UInt, other: UInt) = value.and(other)
@CompileTimeCalculation fun or(value: UInt, other: UInt) = value.or(other)
@CompileTimeCalculation fun xor(value: UInt, other: UInt) = value.xor(other)
@CompileTimeCalculation fun inv(value: UInt) = value.inv()

@CompileTimeCalculation fun toByte(value: UInt) = value.toByte()
@CompileTimeCalculation fun toShort(value: UInt) = value.toShort()
@CompileTimeCalculation fun toInt(value: UInt) = value.toInt()
@CompileTimeCalculation fun toLong(value: UInt) = value.toLong()
@CompileTimeCalculation fun toUByte(value: UInt) = value.toUByte()
@CompileTimeCalculation fun toUShort(value: UInt) = value.toUShort()
@CompileTimeCalculation fun toUInt(value: UInt) = value.toUInt()
@CompileTimeCalculation fun toULong(value: UInt) = value.toULong()
@CompileTimeCalculation fun toFloat(value: UInt) = value.toFloat()
@CompileTimeCalculation fun toDouble(value: UInt) = value.toDouble()

@CompileTimeCalculation fun toString(value: UInt) = value.toString()
@CompileTimeCalculation fun hashCode(value: UInt) = value.hashCode()
@CompileTimeCalculation fun equals(value: UInt, other: Any) = value.equals(other)

@CompileTimeCalculation fun echo(value: Any) = value

const val min = <!EVALUATED: `0`!>echo(UInt.MIN_VALUE) as UInt<!>
const val max = <!EVALUATED: `-1`!>echo(UInt.MAX_VALUE) as UInt<!>
const val bytes = <!EVALUATED: `4`!>echo(UInt.SIZE_BYTES) as Int<!>
const val bits = <!EVALUATED: `32`!>echo(UInt.SIZE_BITS) as Int<!>

const val uByte: UByte = 0u
const val uByteNonZero: UByte = 1u
const val uShort: UShort = 1u
const val uInt: UInt = 2u
const val uLong: ULong = 3uL

const val compare1 = <!EVALUATED: `1`!>compareTo(2u, uByte)<!>
const val compare2 = <!EVALUATED: `1`!>compareTo(2u, uShort)<!>
const val compare3 = <!EVALUATED: `0`!>compareTo(2u, uInt)<!>
const val compare4 = <!EVALUATED: `-1`!>compareTo(2u, uLong)<!>

const val plus1 = <!EVALUATED: `2`!>plus(2u, uByte)<!>
const val plus2 = <!EVALUATED: `3`!>plus(2u, uShort)<!>
const val plus3 = <!EVALUATED: `4`!>plus(2u, uInt)<!>
const val plus4 = <!EVALUATED: `5`!>plus(2u, uLong)<!>

const val minus1 = <!EVALUATED: `2`!>minus(2u, uByte)<!>
const val minus2 = <!EVALUATED: `1`!>minus(2u, uShort)<!>
const val minus3 = <!EVALUATED: `0`!>minus(2u, uInt)<!>
const val minus4 = <!EVALUATED: `-1`!>minus(2u, uLong)<!>

const val times1 = <!EVALUATED: `0`!>times(2u, uByte)<!>
const val times2 = <!EVALUATED: `2`!>times(2u, uShort)<!>
const val times3 = <!EVALUATED: `4`!>times(2u, uInt)<!>
const val times4 = <!EVALUATED: `6`!>times(2u, uLong)<!>

const val div1 = <!EVALUATED: `2`!>div(2u, uByteNonZero)<!>
const val div2 = <!EVALUATED: `2`!>div(2u, uShort)<!>
const val div3 = <!EVALUATED: `1`!>div(2u, uInt)<!>
const val div4 = <!EVALUATED: `0`!>div(2u, uLong)<!>

const val rem1 = <!EVALUATED: `0`!>rem(2u, uByteNonZero)<!>
const val rem2 = <!EVALUATED: `0`!>rem(2u, uShort)<!>
const val rem3 = <!EVALUATED: `0`!>rem(2u, uInt)<!>
const val rem4 = <!EVALUATED: `2`!>rem(2u, uLong)<!>

const val inc = <!EVALUATED: `4`!>inc(3u)<!>
const val dec = <!EVALUATED: `2`!>dec(3u)<!>

const val rangeTo = <!EVALUATED: `10`!>rangeTo(0u, 10u).last<!>

const val shiftLeft = <!EVALUATED: `16`!>shl(8u, 1)<!>
const val shiftRight = <!EVALUATED: `2`!>shr(8u, 2)<!>

const val and = <!EVALUATED: `0`!>and(8u, 1u)<!>
const val or = <!EVALUATED: `10`!>or(8u, 2u)<!>
const val xor = <!EVALUATED: `11`!>xor(8u, 3u)<!>
const val inv = <!EVALUATED: `-9`!>inv(8u)<!>

const val a1 = <!EVALUATED: `1`!>toByte(1u)<!>
const val a2 = <!EVALUATED: `2`!>toShort(2u)<!>
const val a3 = <!EVALUATED: `3`!>toInt(3u)<!>
const val a4 = <!EVALUATED: `4`!>toLong(4u)<!>
const val a5 = <!EVALUATED: `5`!>toUByte(5u)<!>
const val a6 = <!EVALUATED: `6`!>toUShort(6u)<!>
const val a7 = <!EVALUATED: `7`!>toUInt(7u)<!>
const val a8 = <!EVALUATED: `8`!>toULong(8u)<!>
const val a9 = <!EVALUATED: `9.0`!>toFloat(9u)<!>
const val a10 = <!EVALUATED: `10.0`!>toDouble(10u)<!>

const val b1 = <!EVALUATED: `10`!>toString(10u)<!>
// const val b2 = hashCode(10u) TODO support later; in current version method hashCode is missing
const val b3 = <!EVALUATED: `false`!>equals(10u, 11u)<!>
const val b4 = <!EVALUATED: `false`!>equals(1u, 1)<!>
const val b5 = <!EVALUATED: `false`!>equals(3u, 3uL)<!>
