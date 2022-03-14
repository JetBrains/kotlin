// this hack is used to ensure that iterator will be resolved first
@CompileTimeCalculation internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator()
@CompileTimeCalculation public class IntRange(start: Int, endInclusive: Int) : IntProgression(start, endInclusive, 1), ClosedRange<Int>
@CompileTimeCalculation internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator()
@CompileTimeCalculation public class LongRange(start: Long, endInclusive: Long) : LongProgression(start, endInclusive, 1), ClosedRange<Long>

@CompileTimeCalculation fun compareTo(value: Int, other: Byte) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Int, other: Short) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Int, other: Int) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Int, other: Long) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Int, other: Float) = value.compareTo(other)
@CompileTimeCalculation fun compareTo(value: Int, other: Double) = value.compareTo(other)

@CompileTimeCalculation fun plus(value: Int, other: Byte) = value.plus(other)
@CompileTimeCalculation fun plus(value: Int, other: Short) = value.plus(other)
@CompileTimeCalculation fun plus(value: Int, other: Int) = value.plus(other)
@CompileTimeCalculation fun plus(value: Int, other: Long) = value.plus(other)
@CompileTimeCalculation fun plus(value: Int, other: Float) = value.plus(other)
@CompileTimeCalculation fun plus(value: Int, other: Double) = value.plus(other)

@CompileTimeCalculation fun minus(value: Int, other: Byte) = value.minus(other)
@CompileTimeCalculation fun minus(value: Int, other: Short) = value.minus(other)
@CompileTimeCalculation fun minus(value: Int, other: Int) = value.minus(other)
@CompileTimeCalculation fun minus(value: Int, other: Long) = value.minus(other)
@CompileTimeCalculation fun minus(value: Int, other: Float) = value.minus(other)
@CompileTimeCalculation fun minus(value: Int, other: Double) = value.minus(other)

@CompileTimeCalculation fun times(value: Int, other: Byte) = value.times(other)
@CompileTimeCalculation fun times(value: Int, other: Short) = value.times(other)
@CompileTimeCalculation fun times(value: Int, other: Int) = value.times(other)
@CompileTimeCalculation fun times(value: Int, other: Long) = value.times(other)
@CompileTimeCalculation fun times(value: Int, other: Float) = value.times(other)
@CompileTimeCalculation fun times(value: Int, other: Double) = value.times(other)

@CompileTimeCalculation fun div(value: Int, other: Byte) = value.div(other)
@CompileTimeCalculation fun div(value: Int, other: Short) = value.div(other)
@CompileTimeCalculation fun div(value: Int, other: Int) = value.div(other)
@CompileTimeCalculation fun div(value: Int, other: Long) = value.div(other)
@CompileTimeCalculation fun div(value: Int, other: Float) = value.div(other)
@CompileTimeCalculation fun div(value: Int, other: Double) = value.div(other)

@CompileTimeCalculation fun rem(value: Int, other: Byte) = value.rem(other)
@CompileTimeCalculation fun rem(value: Int, other: Short) = value.rem(other)
@CompileTimeCalculation fun rem(value: Int, other: Int) = value.rem(other)
@CompileTimeCalculation fun rem(value: Int, other: Long) = value.rem(other)
@CompileTimeCalculation fun rem(value: Int, other: Float) = value.rem(other)
@CompileTimeCalculation fun rem(value: Int, other: Double) = value.rem(other)

@CompileTimeCalculation fun inc(value: Int) = value.inc()
@CompileTimeCalculation fun dec(value: Int) = value.dec()

@CompileTimeCalculation fun unaryPlus(value: Int) = value.unaryPlus()
@CompileTimeCalculation fun unaryMinus(value: Int) = value.unaryMinus()

@CompileTimeCalculation fun rangeTo(value: Int, other: Byte) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Int, other: Short) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Int, other: Int) = value.rangeTo(other)
@CompileTimeCalculation fun rangeTo(value: Int, other: Long) = value.rangeTo(other)

@CompileTimeCalculation fun shl(value: Int, bitCount: Int) = value.shl(bitCount)
@CompileTimeCalculation fun shr(value: Int, bitCount: Int) = value.shr(bitCount)
@CompileTimeCalculation fun ushr(value: Int, bitCount: Int) = value.ushr(bitCount)

@CompileTimeCalculation fun and(value: Int, other: Int) = value.and(other)
@CompileTimeCalculation fun or(value: Int, other: Int) = value.or(other)
@CompileTimeCalculation fun xor(value: Int, other: Int) = value.xor(other)
@CompileTimeCalculation fun inv(value: Int) = value.inv()

@CompileTimeCalculation fun toByte(value: Int) = value.toByte()
@CompileTimeCalculation fun toChar(value: Int) = value.toChar()
@CompileTimeCalculation fun toShort(value: Int) = value.toShort()
@CompileTimeCalculation fun toInt(value: Int) = value.toInt()
@CompileTimeCalculation fun toLong(value: Int) = value.toLong()
@CompileTimeCalculation fun toFloat(value: Int) = value.toFloat()
@CompileTimeCalculation fun toDouble(value: Int) = value.toDouble()

@CompileTimeCalculation fun toString(value: Int) = value.toString()
@CompileTimeCalculation fun hashCode(value: Int) = value.hashCode()
@CompileTimeCalculation fun equals(value: Int, other: Int) = value.equals(other)

@CompileTimeCalculation fun echo(value: Int) = value

const val min = <!EVALUATED: `-2147483648`!>echo(Int.MIN_VALUE)<!>
const val max = <!EVALUATED: `2147483647`!>echo(Int.MAX_VALUE)<!>
const val bytes = <!EVALUATED: `4`!>echo(Int.SIZE_BYTES)<!>
const val bits = <!EVALUATED: `32`!>echo(Int.SIZE_BITS)<!>

const val compare1 = <!EVALUATED: `1`!>compareTo(5, 1.toByte())<!>
const val compare2 = <!EVALUATED: `1`!>compareTo(5, 2.toShort())<!>
const val compare3 = <!EVALUATED: `1`!>compareTo(5, 3)<!>
const val compare4 = <!EVALUATED: `1`!>compareTo(5, 4L)<!>
const val compare5 = <!EVALUATED: `0`!>compareTo(5, 5.toFloat())<!>
const val compare6 = <!EVALUATED: `-1`!>compareTo(5, 6.toDouble())<!>

const val plus1 = <!EVALUATED: `6`!>plus(5, 1.toByte())<!>
const val plus2 = <!EVALUATED: `7`!>plus(5, 2.toShort())<!>
const val plus3 = <!EVALUATED: `8`!>plus(5, 3)<!>
const val plus4 = <!EVALUATED: `9`!>plus(5, 4L)<!>
const val plus5 = <!EVALUATED: `10.0`!>plus(5, 5.toFloat())<!>
const val plus6 = <!EVALUATED: `11.0`!>plus(5, 6.toDouble())<!>

const val minus1 = <!EVALUATED: `4`!>minus(5, 1.toByte())<!>
const val minus2 = <!EVALUATED: `3`!>minus(5, 2.toShort())<!>
const val minus3 = <!EVALUATED: `2`!>minus(5, 3)<!>
const val minus4 = <!EVALUATED: `1`!>minus(5, 4L)<!>
const val minus5 = <!EVALUATED: `0.0`!>minus(5, 5.toFloat())<!>
const val minus6 = <!EVALUATED: `-1.0`!>minus(5, 6.toDouble())<!>

const val times1 = <!EVALUATED: `5`!>times(5, 1.toByte())<!>
const val times2 = <!EVALUATED: `10`!>times(5, 2.toShort())<!>
const val times3 = <!EVALUATED: `15`!>times(5, 3)<!>
const val times4 = <!EVALUATED: `20`!>times(5, 4L)<!>
const val times5 = <!EVALUATED: `25.0`!>times(5, 5.toFloat())<!>
const val times6 = <!EVALUATED: `30.0`!>times(5, 6.toDouble())<!>

const val div1 = <!EVALUATED: `100`!>div(100, 1.toByte())<!>
const val div2 = <!EVALUATED: `50`!>div(100, 2.toShort())<!>
const val div3 = <!EVALUATED: `25`!>div(100, 4)<!>
const val div4 = <!EVALUATED: `10`!>div(100, 10L)<!>
const val div5 = <!EVALUATED: `4.0`!>div(100, 25.toFloat())<!>
const val div6 = <!EVALUATED: `2.0`!>div(100, 50.toDouble())<!>

const val rem1 = <!EVALUATED: `0`!>rem(5, 1.toByte())<!>
const val rem2 = <!EVALUATED: `1`!>rem(5, 2.toShort())<!>
const val rem3 = <!EVALUATED: `2`!>rem(5, 3)<!>
const val rem4 = <!EVALUATED: `1`!>rem(5, 4L)<!>
const val rem5 = <!EVALUATED: `0.0`!>rem(5, 5.toFloat())<!>
const val rem6 = <!EVALUATED: `5.0`!>rem(5, 6.toDouble())<!>

const val increment = <!EVALUATED: `4`!>inc(3)<!>
const val decrement = <!EVALUATED: `2`!>dec(3)<!>

const val unaryPlus = <!EVALUATED: `3`!>unaryPlus(3)<!>
const val unaryMinus = <!EVALUATED: `-3`!>unaryMinus(3)<!>

const val rangeTo1 = <!EVALUATED: `1`!>rangeTo(5, 1.toByte()).last<!>
const val rangeTo2 = <!EVALUATED: `2`!>rangeTo(5, 2.toShort()).last<!>
const val rangeTo3 = <!EVALUATED: `3`!>rangeTo(5, 3).last<!>
const val rangeTo4 = <!EVALUATED: `4`!>rangeTo(5, 4L).last<!>

const val shiftLeft = <!EVALUATED: `16`!>shl(8, 1)<!>
const val shiftRight = <!EVALUATED: `2`!>shr(8, 2)<!>
const val unsignedShiftRight = <!EVALUATED: `536870911`!>ushr(-8, 3)<!>

const val and = <!EVALUATED: `0`!>and(8, 1)<!>
const val or = <!EVALUATED: `10`!>or(8, 2)<!>
const val xor = <!EVALUATED: `-5`!>xor(-8, 3)<!>
const val inv = <!EVALUATED: `-9`!>inv(8)<!>

const val a1 = <!EVALUATED: `1`!>toByte(1)<!>
const val a2 = <!EVALUATED: ``!>toChar(2)<!>
const val a3 = <!EVALUATED: `3`!>toShort(3)<!>
const val a4 = <!EVALUATED: `4`!>toInt(4)<!>
const val a5 = <!EVALUATED: `5`!>toLong(5)<!>
const val a6 = <!EVALUATED: `6.0`!>toFloat(6)<!>
const val a7 = <!EVALUATED: `7.0`!>toDouble(7)<!>

const val b1 = <!EVALUATED: `10`!>toString(10)<!>
const val b2 = <!EVALUATED: `10`!>hashCode(10)<!>
const val b3 = <!EVALUATED: `false`!>equals(10, 11)<!>
const val b4 = <!EVALUATED: `true`!>equals(1, 1.toInt())<!>
const val b5 = <!EVALUATED: `true`!>equals(1, 1)<!>
