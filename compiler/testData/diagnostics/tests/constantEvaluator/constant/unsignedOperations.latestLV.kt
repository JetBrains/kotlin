// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -REDUNDANT_CALL_OF_CONVERSION_METHOD
const val uint = 456U

const val plus = 1u + 2u
const val minus = 2u - 1u
const val mul = 1u * 2u
const val div = 1u / 2u

const val rem = 1u % 2u
const val floorDiv = 1u.floorDiv(2u)
const val mod = 1u.mod(2u)

const val increment = 1u.inc()
const val decrement = 1u.dec()

const val shl = 1u.shl(1)
const val shr = 1u.shr(1)

const val and = 1u.and(2u)
const val or = 1u.or(2u)
const val xor = 1u.xor(2u)
const val inv = 1u.inv()

const val toByte = 1u.toByte()
const val toShort = 1u.toShort()
const val toInt = 1u.toInt()
const val toLong = 1u.toLong()
const val toFloat = 1u.toFloat()
const val toDouble = 1u.toDouble()

const val toUByte = 1u.toUByte()
const val toUShort = 1u.toUShort()
const val toUInt = 1u.toUInt()
const val toULong = 1u.toULong()

const val toString = 1u.toString()

const val stringPlus1 = 1u.toString() + 2
const val stringPlus2 = "${1u} 2"
const val stringPlus3 = "1" + 2u
const val stringPlus4 = "1" + 2u.toString()

const val compare1 = 1u < 2u
const val compare2 = 1u <= 2u
const val compare3 = 1u > 2u
const val compare4 = 1u >= 2u

const val equal1 = 1u == 1u
const val equal2 = uint == 1u
const val equal3 = 1u == uint

const val notEqual1 = 1u != 1u
const val notEqual2 = uint != 1u
const val notEqual3 = 1u != uint

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, const, equalityExpression, integerLiteral,
propertyDeclaration, stringLiteral, unsignedLiteral */
