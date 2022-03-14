// this hack is used to ensure that iterator will be resolved first
@CompileTimeCalculation internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator()
@CompileTimeCalculation public class LongRange(start: Long, endInclusive: Long) : LongProgression(start, endInclusive, 1), ClosedRange<Long>

@CompileTimeCalculation fun compareTo(value: Long, other: Byte) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Long, other: Short) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Long, other: Int) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Long, other: Long) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Long, other: Float) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Long, other: Double) = value.compareTo(other)

@CompileTimeCalculation fun plus(value: Long, other: Byte) = value.plus(other)
@CompileTimeCalculation fun plus(value: Long, other: Short) = value.plus(other)
@CompileTimeCalculation fun plus(value: Long, other: Int) = value.plus(other)
@CompileTimeCalculation fun plus(value: Long, other: Long) = value.plus(other)
@CompileTimeCalculation fun plus(value: Long, other: Float) = value.plus(other)
@CompileTimeCalculation fun plus(value: Long, other: Double) = value.plus(other)

@CompileTimeCalculation fun minus(value: Long, other: Byte) = value.minus(other)
@CompileTimeCalculation fun minus(value: Long, other: Short) = value.minus(other)
@CompileTimeCalculation fun minus(value: Long, other: Int) = value.minus(other)
@CompileTimeCalculation fun minus(value: Long, other: Long) = value.minus(other)
@CompileTimeCalculation fun minus(value: Long, other: Float) = value.minus(other)
@CompileTimeCalculation fun minus(value: Long, other: Double) = value.minus(other)

@CompileTimeCalculation fun times(value: Long, other: Byte) = value.times(other)
@CompileTimeCalculation fun times(value: Long, other: Short) = value.times(other)
@CompileTimeCalculation fun times(value: Long, other: Int) = value.times(other)
@CompileTimeCalculation fun times(value: Long, other: Long) = value.times(other)
@CompileTimeCalculation fun times(value: Long, other: Float) = value.times(other)
@CompileTimeCalculation fun times(value: Long, other: Double) = value.times(other)

@CompileTimeCalculation fun div(value: Long, other: Byte) = value.div(other)
@CompileTimeCalculation fun div(value: Long, other: Short) = value.div(other)
@CompileTimeCalculation fun div(value: Long, other: Int) = value.div(other)
@CompileTimeCalculation fun div(value: Long, other: Long) = value.div(other)
@CompileTimeCalculation fun div(value: Long, other: Float) = value.div(other)
@CompileTimeCalculation fun div(value: Long, other: Double) = value.div(other)

@CompileTimeCalculation fun rem(value: Long, other: Byte) = value.rem(other)
@CompileTimeCalculation fun rem(value: Long, other: Short) = value.rem(other)
@CompileTimeCalculation fun rem(value: Long, other: Int) = value.rem(other)
@CompileTimeCalculation fun rem(value: Long, other: Long) = value.rem(other)
@CompileTimeCalculation fun rem(value: Long, other: Float) = value.rem(other)
@CompileTimeCalculation fun rem(value: Long, other: Double) = value.rem(other)

@CompileTimeCalculation fun inc(value: Long) = value.inc()
@CompileTimeCalculation fun dec(value: Long) = value.dec()

@CompileTimeCalculation fun unaryPlus(value: Long) = value.unaryPlus()
@CompileTimeCalculation fun unaryMinus(value: Long) = value.unaryMinus()

@CompileTimeCalculation fun rangeTo(value: Long, other: Byte) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Long, other: Short) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Long, other: Int) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Long, other: Long) = value.rangeTo(other)

@CompileTimeCalculation fun shl(value: Long, bitCount: Int) = value.shl(bitCount)
@CompileTimeCalculation fun shr(value: Long, bitCount: Int) = value.shr(bitCount)
@CompileTimeCalculation fun ushr(value: Long, bitCount: Int) = value.ushr(bitCount)

@CompileTimeCalculation fun and(value: Long, other: Long) = value.and(other)
@CompileTimeCalculation fun or(value: Long, other: Long) = value.or(other)
@CompileTimeCalculation fun xor(value: Long, other: Long) = value.xor(other)
@CompileTimeCalculation fun inv(value: Long) = value.inv()

@CompileTimeCalculation fun toByte(value: Long) = value.toByte()
@CompileTimeCalculation fun toChar(value: Long) = value.toChar()
@CompileTimeCalculation fun toShort(value: Long) = value.toShort()
@CompileTimeCalculation fun toInt(value: Long) = value.toInt()
@CompileTimeCalculation fun toLong(value: Long) = value.toLong()
@CompileTimeCalculation fun toFloat(value: Long) = value.toFloat()
@CompileTimeCalculation fun toDouble(value: Long) = value.toDouble()

@CompileTimeCalculation fun toString(value: Long) = value.toString()
@CompileTimeCalculation fun hashCode(value: Long) = value.hashCode()
@CompileTimeCalculation fun equals(value: Long, other: Long) = value.equals(other)

@CompileTimeCalculation fun echo(value: Any) = value

const val min = <!EVALUATED: `-9223372036854775808`!>echo(Long.MIN_VALUE) as Long<!>
const val max = <!EVALUATED: `9223372036854775807`!>echo(Long.MAX_VALUE) as Long<!>
const val bytes = <!EVALUATED: `8`!>echo(Long.SIZE_BYTES) as Int<!>
const val bits = <!EVALUATED: `64`!>echo(Long.SIZE_BITS) as Int<!>

const val compare1 = <!EVALUATED: `1`!>compareTo(5L, 1.toByte())<!>
const val compare2 = <!EVALUATED: `1`!>compareTo(5L, 2.toShort())<!>
const val compare3 = <!EVALUATED: `1`!>compareTo(5L, 3)<!>
const val compare4 = <!EVALUATED: `1`!>compareTo(5L, 4L)<!>
const val compare5 = <!EVALUATED: `0`!>compareTo(5L, 5.toFloat())<!>
const val compare6 = <!EVALUATED: `-1`!>compareTo(5L, 6.toDouble())<!>

const val plus1 = <!EVALUATED: `6`!>plus(5L, 1.toByte())<!>
const val plus2 = <!EVALUATED: `7`!>plus(5L, 2.toShort())<!>
const val plus3 = <!EVALUATED: `8`!>plus(5L, 3)<!>
const val plus4 = <!EVALUATED: `9`!>plus(5L, 4L)<!>
const val plus5 = <!EVALUATED: `10.0`!>plus(5L, 5.toFloat())<!>
const val plus6 = <!EVALUATED: `11.0`!>plus(5L, 6.toDouble())<!>

const val minus1 = <!EVALUATED: `4`!>minus(5L, 1.toByte())<!>
const val minus2 = <!EVALUATED: `3`!>minus(5L, 2.toShort())<!>
const val minus3 = <!EVALUATED: `2`!>minus(5L, 3)<!>
const val minus4 = <!EVALUATED: `1`!>minus(5L, 4L)<!>
const val minus5 = <!EVALUATED: `0.0`!>minus(5L, 5.toFloat())<!>
const val minus6 = <!EVALUATED: `-1.0`!>minus(5L, 6.toDouble())<!>

const val times1 = <!EVALUATED: `5`!>times(5L, 1.toByte())<!>
const val times2 = <!EVALUATED: `10`!>times(5L, 2.toShort())<!>
const val times3 = <!EVALUATED: `15`!>times(5L, 3)<!>
const val times4 = <!EVALUATED: `20`!>times(5L, 4L)<!>
const val times5 = <!EVALUATED: `25.0`!>times(5L, 5.toFloat())<!>
const val times6 = <!EVALUATED: `30.0`!>times(5L, 6.toDouble())<!>

const val div1 = <!EVALUATED: `100`!>div(100L, 1.toByte())<!>
const val div2 = <!EVALUATED: `50`!>div(100L, 2.toShort())<!>
const val div3 = <!EVALUATED: `25`!>div(100L, 4)<!>
const val div4 = <!EVALUATED: `10`!>div(100L, 10L)<!>
const val div5 = <!EVALUATED: `4.0`!>div(100L, 25.toFloat())<!>
const val div6 = <!EVALUATED: `2.0`!>div(100L, 50.toDouble())<!>

const val rem1 = <!EVALUATED: `0`!>rem(5L, 1.toByte())<!>
const val rem2 = <!EVALUATED: `1`!>rem(5L, 2.toShort())<!>
const val rem3 = <!EVALUATED: `2`!>rem(5L, 3)<!>
const val rem4 = <!EVALUATED: `1`!>rem(5L, 4L)<!>
const val rem5 = <!EVALUATED: `0.0`!>rem(5L, 5.toFloat())<!>
const val rem6 = <!EVALUATED: `5.0`!>rem(5L, 6.toDouble())<!>

const val increment = <!EVALUATED: `4`!>inc(3L)<!>
const val decrement = <!EVALUATED: `2`!>dec(3L)<!>

const val unaryPlus = <!EVALUATED: `3`!>unaryPlus(3L)<!>
const val unaryMinus = <!EVALUATED: `-3`!>unaryMinus(3L)<!>

const val rangeTo1 = <!EVALUATED: `1`!>rangeTo(5L, 1.toByte()).last<!>
const val rangeTo2 = <!EVALUATED: `2`!>rangeTo(5L, 2.toShort()).last<!>
const val rangeTo3 = <!EVALUATED: `3`!>rangeTo(5L, 3).last<!>
const val rangeTo4 = <!EVALUATED: `4`!>rangeTo(5L, 4L).last<!>

const val shiftLeft = <!EVALUATED: `16`!>shl(8L, 1)<!>
const val shiftRight = <!EVALUATED: `2`!>shr(8L, 2)<!>
const val unsignedShiftRight = <!EVALUATED: `2305843009213693951`!>ushr(-8L, 3)<!>

const val and = <!EVALUATED: `0`!>and(8L, 1L)<!>
const val or = <!EVALUATED: `10`!>or(8L, 2L)<!>
const val xor = <!EVALUATED: `-5`!>xor(-8L, 3L)<!>
const val inv = <!EVALUATED: `-9`!>inv(8L)<!>

const val a1 = <!EVALUATED: `1`!>toByte(1L)<!>
const val a2 = <!EVALUATED: ``!>toChar(2L)<!>
const val a3 = <!EVALUATED: `3`!>toShort(3L)<!>
const val a4 = <!EVALUATED: `4`!>toInt(4L)<!>
const val a5 = <!EVALUATED: `5`!>toLong(5L)<!>
const val a6 = <!EVALUATED: `6.0`!>toFloat(6L)<!>
const val a7 = <!EVALUATED: `7.0`!>toDouble(7L)<!>

const val b1 = <!EVALUATED: `10`!>toString(10L)<!>
const val b2 = <!EVALUATED: `10`!>hashCode(10L)<!>
const val b3 = <!EVALUATED: `false`!>equals(10L, 11L)<!>
const val b4 = <!EVALUATED: `true`!>equals(1L, 1.toLong())<!>
const val b5 = <!EVALUATED: `true`!>equals(1L, 1)<!>
