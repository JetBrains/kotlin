// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -REDUNDANT_CALL_OF_CONVERSION_METHOD
const val byteVal: UByte = 1u
const val shortVal: UShort = 2u
const val intVal: UInt = 3u
const val longVal: ULong = 4uL

const val compareTo1 = byteVal.compareTo(byteVal)
const val compareTo2 = byteVal.compareTo(shortVal)
const val compareTo3 = byteVal.compareTo(intVal)
const val compareTo4 = byteVal.compareTo(longVal)

const val plus1 = byteVal.plus(byteVal)
const val plus2 = byteVal.plus(shortVal)
const val plus3 = byteVal.plus(intVal)
const val plus4 = byteVal.plus(longVal)

const val minus1 = byteVal.minus(byteVal)
const val minus2 = byteVal.minus(shortVal)
const val minus3 = byteVal.minus(intVal)
const val minus4 = byteVal.minus(longVal)

const val times1 = byteVal.times(byteVal)
const val times2 = byteVal.times(shortVal)
const val times3 = byteVal.times(intVal)
const val times4 = byteVal.times(longVal)

const val div1 = byteVal.div(byteVal)
const val div2 = byteVal.div(shortVal)
const val div3 = byteVal.div(intVal)
const val div4 = byteVal.div(longVal)

const val rem1 = byteVal.rem(byteVal)
const val rem2 = byteVal.rem(shortVal)
const val rem3 = byteVal.rem(intVal)
const val rem4 = byteVal.rem(longVal)

const val floorDiv1 = byteVal.floorDiv(byteVal)
const val floorDiv2 = byteVal.floorDiv(shortVal)
const val floorDiv3 = byteVal.floorDiv(intVal)
const val floorDiv4 = byteVal.floorDiv(longVal)

const val mod1 = byteVal.mod(byteVal)
const val mod2 = byteVal.mod(shortVal)
const val mod3 = byteVal.mod(intVal)
const val mod4 = byteVal.mod(longVal)

const val and = byteVal.and(byteVal)
const val or = byteVal.or(byteVal)
const val xor = byteVal.xor(byteVal)
const val inv = byteVal.inv()

const val convert1 = byteVal.toByte()
const val convert2 = byteVal.toShort()
const val convert3 = byteVal.toInt()
const val convert4 = byteVal.toLong()
const val convert5 = byteVal.toFloat()
const val convert6 = byteVal.toDouble()
const val convert7 = byteVal.toUByte()
const val convert8 = byteVal.toUShort()
const val convert9 = byteVal.toUInt()
const val convert10 = byteVal.toULong()

const val toString1 = byteVal.toString()
const val toString2 = shortVal.toString()
const val toString3 = intVal.toString()
const val toString4 = longVal.toString()

const val equals1 = byteVal.equals(byteVal)
const val equals2 = byteVal.equals(shortVal)
const val equals3 = byteVal.equals(intVal)
const val equals4 = byteVal.equals(longVal)

/* GENERATED_FIR_TAGS: const, propertyDeclaration, unsignedLiteral */
