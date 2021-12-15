// FIR_IDENTICAL
// WITH_STDLIB
const val byteVal: UByte = 1u
const val shortVal: UShort = 2u
const val intVal: UInt = 3u
const val longVal: ULong = 4uL

const val compareTo1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.compareTo(byteVal)<!>
const val compareTo2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.compareTo(shortVal)<!>
const val compareTo3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.compareTo(intVal)<!>
const val compareTo4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.compareTo(longVal)<!>

const val plus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.plus(byteVal)<!>
const val plus2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.plus(shortVal)<!>
const val plus3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.plus(intVal)<!>
const val plus4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.plus(longVal)<!>

const val minus1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.minus(byteVal)<!>
const val minus2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.minus(shortVal)<!>
const val minus3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.minus(intVal)<!>
const val minus4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.minus(longVal)<!>

const val times1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.times(byteVal)<!>
const val times2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.times(shortVal)<!>
const val times3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.times(intVal)<!>
const val times4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.times(longVal)<!>

const val div1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.div(byteVal)<!>
const val div2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.div(shortVal)<!>
const val div3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.div(intVal)<!>
const val div4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.div(longVal)<!>

const val rem1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.rem(byteVal)<!>
const val rem2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.rem(shortVal)<!>
const val rem3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.rem(intVal)<!>
const val rem4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.rem(longVal)<!>

const val floorDiv1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.floorDiv(byteVal)<!>
const val floorDiv2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.floorDiv(shortVal)<!>
const val floorDiv3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.floorDiv(intVal)<!>
const val floorDiv4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.floorDiv(longVal)<!>

const val mod1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.mod(byteVal)<!>
const val mod2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.mod(shortVal)<!>
const val mod3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.mod(intVal)<!>
const val mod4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.mod(longVal)<!>

const val and = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.and(byteVal)<!>
const val or = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.or(byteVal)<!>
const val xor = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.xor(byteVal)<!>
const val inv = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.inv()<!>

const val convert1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toByte()<!>
const val convert2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toShort()<!>
const val convert3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toInt()<!>
const val convert4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toLong()<!>
const val convert5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toFloat()<!>
const val convert6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toDouble()<!>
const val convert7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toUByte()<!>
const val convert8 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toUShort()<!>
const val convert9 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toUInt()<!>
const val convert10 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toULong()<!>

const val toString1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.toString()<!>
const val toString2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>shortVal.toString()<!>
const val toString3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>intVal.toString()<!>
const val toString4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>longVal.toString()<!>

const val equals1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.equals(byteVal)<!>
const val equals2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.equals(shortVal)<!>
const val equals3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.equals(intVal)<!>
const val equals4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>byteVal.equals(longVal)<!>
