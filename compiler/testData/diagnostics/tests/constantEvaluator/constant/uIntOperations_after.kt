// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// DIAGNOSTICS: -REDUNDANT_CALL_OF_CONVERSION_METHOD
// WITH_STDLIB
const val zeroVal = 0u
const val oneVal = 1u
const val twoVal = 2u
const val threeVal = 3u
const val fourVal = 4u

const val byteVal = 2.toByte()
const val shortVal = 2.toShort()
const val intVal = 2
const val longVal = 2L
const val ubyteVal = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2u.toUByte()<!>
const val ushortVal = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2u.toUShort()<!>
const val uintVal = 2u
const val ulongVal = 2UL
const val floatVal = 2.0f
const val doubleVal = 2.0

const val compareTo1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.compareTo(twoVal)<!>
const val compareTo2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.compareTo(twoVal)<!>
const val compareTo3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.compareTo(twoVal)<!>
const val compareTo4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.compareTo(ubyteVal)<!>
const val compareTo5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.compareTo(ushortVal)<!>
const val compareTo6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.compareTo(ulongVal)<!>

const val plus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.plus(twoVal)<!>
const val plus2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.plus(twoVal)<!>
const val plus3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.plus(twoVal)<!>
const val plus4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.plus(ubyteVal)<!>
const val plus5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.plus(ushortVal)<!>
const val plus6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.plus(ulongVal)<!>

const val minus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.minus(twoVal)<!>
const val minus2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.minus(twoVal)<!>
const val minus3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.minus(twoVal)<!>
const val minus4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.minus(ubyteVal)<!>
const val minus5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.minus(ushortVal)<!>
const val minus6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.minus(ulongVal)<!>

const val times1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.times(twoVal)<!>
const val times2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.times(twoVal)<!>
const val times3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.times(twoVal)<!>
const val times4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.times(ubyteVal)<!>
const val times5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.times(ushortVal)<!>
const val times6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.times(ulongVal)<!>

const val div1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.div(twoVal)<!>
const val div2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.div(twoVal)<!>
const val div3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.div(twoVal)<!>
const val div4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.div(ubyteVal)<!>
const val div5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.div(ushortVal)<!>
const val div6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.div(ulongVal)<!>

const val floorDiv1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.floorDiv(twoVal)<!>
const val floorDiv2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.floorDiv(twoVal)<!>
const val floorDiv3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.floorDiv(twoVal)<!>
const val floorDiv4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.floorDiv(ubyteVal)<!>
const val floorDiv5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.floorDiv(ushortVal)<!>
const val floorDiv6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.floorDiv(ulongVal)<!>

const val rem1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.rem(twoVal)<!>
const val rem2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.rem(twoVal)<!>
const val rem3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.rem(twoVal)<!>
const val rem4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.rem(ubyteVal)<!>
const val rem5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.rem(ushortVal)<!>
const val rem6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.rem(ulongVal)<!>

const val mod1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.mod(twoVal)<!>
const val mod2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.mod(twoVal)<!>
const val mod3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.mod(twoVal)<!>
const val mod4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.mod(ubyteVal)<!>
const val mod5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.mod(ushortVal)<!>
const val mod6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.mod(ulongVal)<!>

const val and1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.and(twoVal)<!>
const val and2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.and(twoVal)<!>
const val and3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.and(twoVal)<!>
const val and4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12u.and(10u)<!>

const val or1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.or(twoVal)<!>
const val or2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.or(twoVal)<!>
const val or3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.or(twoVal)<!>
const val or4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12u.or(10u)<!>

const val xor1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.xor(twoVal)<!>
const val xor2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.xor(twoVal)<!>
const val xor3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.xor(twoVal)<!>
const val xor4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12u.xor(10u)<!>

const val inv1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>zeroVal.inv()<!>
const val inv2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.inv()<!>

const val shl1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shl(1)<!>
const val shl2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.shl(2)<!>
const val shl3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.shl(2)<!>
const val shl4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shl(31)<!>
const val shl5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shl(32)<!>
const val shl6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shl(63)<!>

const val shr1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shr(1)<!>
const val shr2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.shr(1)<!>
const val shr3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.shr(1)<!>
const val shr4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shr(31)<!>
const val shr5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shr(32)<!>
const val shr6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.shr(63)<!>

const val convert1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toUByte()<!>
const val convert2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toUShort()<!>
const val convert3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toUInt()<!>
const val convert4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toULong()<!>
const val convert5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toFloat()<!>
const val convert6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toDouble()<!>
const val convert7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toByte()<!>
const val convert8 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toShort()<!>
const val convert9 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toInt()<!>
const val convert10 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toLong()<!>
const val convert11 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toByte().toUInt()<!>
const val convert12 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toShort().toUInt()<!>
const val convert13 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toUInt()<!>
const val convert14 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1L.toUInt()<!>
const val convert15 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0f.toUInt()<!>
const val convert16 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0.toUInt()<!>

const val equals1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal == twoVal<!>
const val equals2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal == twoVal<!>
const val equals3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal == twoVal<!>
const val equals4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>fourVal == twoVal<!>

const val toString1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.toString()<!>
const val toString2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.toString()<!>

const val limits1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>4294967294u+oneVal<!>
const val limits2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>4294967295u+oneVal<!>
const val limits3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>zeroVal-oneVal<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
