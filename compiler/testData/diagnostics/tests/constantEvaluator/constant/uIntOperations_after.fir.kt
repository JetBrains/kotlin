// RUN_PIPELINE_TILL: FRONTEND
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
const val ubyteVal = 2u.toUByte()
const val ushortVal = 2u.toUShort()
const val uintVal = 2u
const val ulongVal = 2UL
const val floatVal = 2.0f
const val doubleVal = 2.0

const val compareTo1 = oneVal.compareTo(twoVal)
const val compareTo2 = twoVal.compareTo(twoVal)
const val compareTo3 = threeVal.compareTo(twoVal)
const val compareTo4 = twoVal.compareTo(ubyteVal)
const val compareTo5 = twoVal.compareTo(ushortVal)
const val compareTo6 = twoVal.compareTo(ulongVal)

const val plus1 = oneVal.plus(twoVal)
const val plus2 = twoVal.plus(twoVal)
const val plus3 = threeVal.plus(twoVal)
const val plus4 = twoVal.plus(ubyteVal)
const val plus5 = twoVal.plus(ushortVal)
const val plus6 = twoVal.plus(ulongVal)

const val minus1 = oneVal.minus(twoVal)
const val minus2 = twoVal.minus(twoVal)
const val minus3 = threeVal.minus(twoVal)
const val minus4 = twoVal.minus(ubyteVal)
const val minus5 = twoVal.minus(ushortVal)
const val minus6 = twoVal.minus(ulongVal)

const val times1 = oneVal.times(twoVal)
const val times2 = twoVal.times(twoVal)
const val times3 = threeVal.times(twoVal)
const val times4 = twoVal.times(ubyteVal)
const val times5 = twoVal.times(ushortVal)
const val times6 = twoVal.times(ulongVal)

const val div1 = oneVal.div(twoVal)
const val div2 = twoVal.div(twoVal)
const val div3 = threeVal.div(twoVal)
const val div4 = twoVal.div(ubyteVal)
const val div5 = twoVal.div(ushortVal)
const val div6 = twoVal.div(ulongVal)

const val floorDiv1 = oneVal.floorDiv(twoVal)
const val floorDiv2 = twoVal.floorDiv(twoVal)
const val floorDiv3 = threeVal.floorDiv(twoVal)
const val floorDiv4 = twoVal.floorDiv(ubyteVal)
const val floorDiv5 = twoVal.floorDiv(ushortVal)
const val floorDiv6 = twoVal.floorDiv(ulongVal)

const val rem1 = oneVal.rem(twoVal)
const val rem2 = twoVal.rem(twoVal)
const val rem3 = threeVal.rem(twoVal)
const val rem4 = twoVal.rem(ubyteVal)
const val rem5 = twoVal.rem(ushortVal)
const val rem6 = twoVal.rem(ulongVal)

const val mod1 = oneVal.mod(twoVal)
const val mod2 = twoVal.mod(twoVal)
const val mod3 = threeVal.mod(twoVal)
const val mod4 = twoVal.mod(ubyteVal)
const val mod5 = twoVal.mod(ushortVal)
const val mod6 = twoVal.mod(ulongVal)

const val and1 = oneVal.and(twoVal)
const val and2 = twoVal.and(twoVal)
const val and3 = threeVal.and(twoVal)
const val and4 = 12u.and(10u)

const val or1 = oneVal.or(twoVal)
const val or2 = twoVal.or(twoVal)
const val or3 = threeVal.or(twoVal)
const val or4 = 12u.or(10u)

const val xor1 = oneVal.xor(twoVal)
const val xor2 = twoVal.xor(twoVal)
const val xor3 = threeVal.xor(twoVal)
const val xor4 = 12u.xor(10u)

const val inv1 = zeroVal.inv()
const val inv2 = oneVal.inv()

const val shl1 = oneVal.shl(1)
const val shl2 = twoVal.shl(2)
const val shl3 = threeVal.shl(2)
const val shl4 = oneVal.shl(31)
const val shl5 = oneVal.shl(32)
const val shl6 = oneVal.shl(63)

const val shr1 = oneVal.shr(1)
const val shr2 = twoVal.shr(1)
const val shr3 = threeVal.shr(1)
const val shr4 = oneVal.shr(31)
const val shr5 = oneVal.shr(32)
const val shr6 = oneVal.shr(63)

const val convert1 = oneVal.toUByte()
const val convert2 = oneVal.toUShort()
const val convert3 = oneVal.toUInt()
const val convert4 = oneVal.toULong()
const val convert5 = oneVal.toFloat()
const val convert6 = oneVal.toDouble()
const val convert7 = oneVal.toByte()
const val convert8 = oneVal.toShort()
const val convert9 = oneVal.toInt()
const val convert10 = oneVal.toLong()
const val convert11 = 1.toByte().toUInt()
const val convert12 = 1.toShort().toUInt()
const val convert13 = 1.toUInt()
const val convert14 = 1L.toUInt()
const val convert15 = 1.0f.toUInt()
const val convert16 = 1.0.toUInt()

const val equals1 = oneVal == twoVal
const val equals2 = twoVal == twoVal
const val equals3 = threeVal == twoVal
const val equals4 = fourVal == twoVal

const val toString1 = oneVal.toString()
const val toString2 = twoVal.toString()

const val limits1 = 4294967294u+oneVal
const val limits2 = 4294967295u+oneVal
const val limits3 = zeroVal-oneVal

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
