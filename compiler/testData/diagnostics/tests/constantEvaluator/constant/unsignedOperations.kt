// FIR_IDENTICAL
// WITH_STDLIB
const val uint = 456U

const val plus = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u + 2u<!>
const val minus = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2u - 1u<!>
const val mul = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u * 2u<!>
const val div = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u / 2u<!>

const val rem = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u % 2u<!>
const val floorDiv = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.floorDiv(2u)<!>
const val mod = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.mod(2u)<!>

const val increment = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.inc()<!>
const val decrement = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.dec()<!>

const val shl = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.shl(1)<!>
const val shr = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.shr(1)<!>

const val and = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.and(2u)<!>
const val or = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.or(2u)<!>
const val xor = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.xor(2u)<!>
const val inv = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.inv()<!>

const val toByte = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toByte()<!>
const val toShort = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toShort()<!>
const val toInt = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toInt()<!>
const val toLong = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toLong()<!>
const val toFloat = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toFloat()<!>
const val toDouble = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toDouble()<!>

const val toUByte = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toUByte()<!>
const val toUShort = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toUShort()<!>
const val toUInt = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toUInt()<!>
const val toULong = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toULong()<!>

const val toString = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toString()<!>

const val stringPlus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toString() + 2<!>
const val stringPlus2 = "${1u} 2"
const val stringPlus3 = "1" + 2u
const val stringPlus4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"1" + 2u.toString()<!>

const val compare1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u < 2u<!>
const val compare2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u <= 2u<!>
const val compare3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u > 2u<!>
const val compare4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u >= 2u<!>

const val equal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u == 1u<!>
const val equal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>uint == 1u<!>
const val equal3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u == uint<!>

const val notEqual1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u != 1u<!>
const val notEqual2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>uint != 1u<!>
const val notEqual3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u != uint<!>
