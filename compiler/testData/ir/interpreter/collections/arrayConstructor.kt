import kotlin.*

const val intArray1 = IntArray(42).<!EVALUATED: `42`!>size<!>
const val intArray2 = <!EVALUATED: `0`!>IntArray(42)[0]<!>
const val intArray3 = <!EVALUATED: `42`!>IntArray(10) { 42 }[0]<!>
const val intArray4 = <!EVALUATED: `7`!>IntArray(10) { it -> it }[7]<!>

const val floatArray1 = FloatArray(42).<!EVALUATED: `42`!>size<!>
const val floatArray2 = <!EVALUATED: `0.0`!>FloatArray(42)[0]<!>
const val floatArray3 = <!EVALUATED: `42.5`!>FloatArray(10) { 42.5f }[0]<!>
const val floatArray4 = <!EVALUATED: `7.0`!>FloatArray(10) { it -> it.toFloat() }[7]<!>

const val array = Array<Any?>(4) {
    when(it) {
        0 -> 1
        1 -> 2.0
        2 -> "3"
        3 -> null
        else -> throw IllegalArgumentException("$it is wrong")
    }
}.<!EVALUATED: `1 2.0 3 null`!>let { it[0].toString() + " " + it[1] + " " + it[2] + " " + it[3] }<!>
