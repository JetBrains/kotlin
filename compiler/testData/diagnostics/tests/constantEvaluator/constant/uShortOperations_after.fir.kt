// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// DIAGNOSTICS: -REDUNDANT_CALL_OF_CONVERSION_METHOD
// WITH_STDLIB
const val zeroVal = 0u.toUShort()
const val oneVal = 1u.toUShort()
const val twoVal = 2u.toUShort()
const val threeVal = 3u.toUShort()
const val fourVal = 4u.toUShort()

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
const val compareTo5 = twoVal.compareTo(uintVal)
const val compareTo6 = twoVal.compareTo(ulongVal)

const val plus1 = oneVal.plus(twoVal)
const val plus2 = twoVal.plus(twoVal)
const val plus3 = threeVal.plus(twoVal)
const val plus4 = twoVal.plus(ubyteVal)
const val plus5 = twoVal.plus(uintVal)
const val plus6 = twoVal.plus(ulongVal)

const val minus1 = oneVal.minus(twoVal)
const val minus2 = twoVal.minus(twoVal)
const val minus3 = threeVal.minus(twoVal)
const val minus4 = twoVal.minus(ubyteVal)
const val minus5 = twoVal.minus(uintVal)
const val minus6 = twoVal.minus(ulongVal)

const val times1 = oneVal.times(twoVal)
const val times2 = twoVal.times(twoVal)
const val times3 = threeVal.times(twoVal)
const val times4 = twoVal.times(ubyteVal)
const val times5 = twoVal.times(uintVal)
const val times6 = twoVal.times(ulongVal)

const val div1 = oneVal.div(twoVal)
const val div2 = twoVal.div(twoVal)
const val div3 = threeVal.div(twoVal)
const val div4 = twoVal.div(ubyteVal)
const val div5 = twoVal.div(uintVal)
const val div6 = twoVal.div(ulongVal)

const val floorDiv1 = oneVal.floorDiv(twoVal)
const val floorDiv2 = twoVal.floorDiv(twoVal)
const val floorDiv3 = threeVal.floorDiv(twoVal)
const val floorDiv4 = twoVal.floorDiv(ubyteVal)
const val floorDiv5 = twoVal.floorDiv(uintVal)
const val floorDiv6 = twoVal.floorDiv(ulongVal)

const val rem1 = oneVal.rem(twoVal)
const val rem2 = twoVal.rem(twoVal)
const val rem3 = threeVal.rem(twoVal)
const val rem4 = twoVal.rem(ubyteVal)
const val rem5 = twoVal.rem(uintVal)
const val rem6 = twoVal.rem(ulongVal)

const val mod1 = oneVal.mod(twoVal)
const val mod2 = twoVal.mod(twoVal)
const val mod3 = threeVal.mod(twoVal)
const val mod4 = twoVal.mod(ubyteVal)
const val mod5 = twoVal.mod(uintVal)
const val mod6 = twoVal.mod(ulongVal)

const val and1 = oneVal.and(twoVal)
const val and2 = twoVal.and(twoVal)
const val and3 = threeVal.and(twoVal)
const val and4 = 12u.toUShort().and(10u.toUShort())

const val or1 = oneVal.or(twoVal)
const val or2 = twoVal.or(twoVal)
const val or3 = threeVal.or(twoVal)
const val or4 = 12u.toUShort().or(10u.toUShort())

const val xor1 = oneVal.xor(twoVal)
const val xor2 = twoVal.xor(twoVal)
const val xor3 = threeVal.xor(twoVal)
const val xor4 = 12u.toUShort().xor(10u.toUShort())

const val inv1 = zeroVal.inv()
const val inv2 = oneVal.inv()

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
const val convert11 = byteVal.toUShort()
const val convert12 = shortVal.toUShort()
const val convert13 = intVal.toUShort()
const val convert14 = longVal.toUShort()

const val equals1 = oneVal == twoVal
const val equals2 = twoVal == twoVal
const val equals3 = threeVal == twoVal
const val equals4 = fourVal == twoVal

const val toString1 = oneVal.toString()
const val toString2 = twoVal.toString()

const val limits1 = 65534u.toUShort()+oneVal
const val limits2 = 65535u.toUShort()+oneVal
const val limits3 = zeroVal-oneVal

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
