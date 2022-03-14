// this hack is used to ensure that iterator will be resolved first
@CompileTimeCalculation internal class CharProgressionIterator(first: Char, last: Char, val step: Int) : CharIterator()
@CompileTimeCalculation public class CharRange(start: Char, endInclusive: Char) : CharProgression(start, endInclusive, 1), ClosedRange<Char>

fun compareTo(first: Char, second: Char) = first.compareTo(second)

fun plus(first: Char, second: Int) = first.plus(second)

fun minus(first: Char, second: Char) = first.minus(second)
fun minus(first: Char, second: Int) = first.minus(second)

fun inc(first: Char) = first.inc()
fun dec(first: Char) = first.dec()

fun rangeTo(first: Char, second: Char) = first.rangeTo(second)

fun toByte(first: Char) = first.toByte()
fun toChar(first: Char) = first.toChar()
fun toShort(first: Char) = first.toShort()
fun toInt(first: Char) = first.toInt()
fun toLong(first: Char) = first.toLong()
fun toFloat(first: Char) = first.toFloat()
fun toDouble(first: Char) = first.toDouble()

fun toString(first: Char) = first.toString()
fun hashCode(first: Char) = first.hashCode()
fun equals(first: Char, second: Char) = first.equals(second)

const val a1 = <!EVALUATED: `-1`!>compareTo('a', 'b')<!>
const val a2 = <!EVALUATED: `3`!>plus('1', 2)<!>
const val a3 = <!EVALUATED: `8`!>minus('9', '1')<!>
const val a4 = <!EVALUATED: `8`!>minus('9', 1)<!>
const val a5 = <!EVALUATED: `2`!>inc('1')<!>
const val a6 = <!EVALUATED: `0`!>dec('1')<!>
const val a7 = <!EVALUATED: `1`!>rangeTo('9', '1').last<!>

const val b1 = <!EVALUATED: `49`!>toByte('1')<!>
const val b2 = <!EVALUATED: `2`!>toChar('2')<!>
const val b3 = <!EVALUATED: `51`!>toShort('3')<!>
const val b4 = <!EVALUATED: `52`!>toInt('4')<!>
const val b5 = <!EVALUATED: `53`!>toLong('5')<!>
const val b6 = <!EVALUATED: `54.0`!>toFloat('6')<!>
const val b7 = <!EVALUATED: `55.0`!>toDouble('7')<!>

const val c1 = <!EVALUATED: `q`!>toString('q')<!>
const val c2 = <!EVALUATED: `113`!>hashCode('q')<!>
const val c3 = <!EVALUATED: `false`!>equals('1', '2')<!>
