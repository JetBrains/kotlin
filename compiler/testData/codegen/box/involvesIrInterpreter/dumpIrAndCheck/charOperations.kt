// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// `Char.equals(Char)` will not be evaluated in K1
// IGNORE_BACKEND_K1: NATIVE
// WITH_STDLIB

const val oneVal = '1'
const val twoVal = '2'
const val threeVal = '3'
const val fourVal = '4'

const val intVal = 5

const val compareTo1 = oneVal.compareTo(twoVal)
const val compareTo2 = twoVal.compareTo(twoVal)
const val compareTo3 = threeVal.compareTo(twoVal)
const val compareTo4 = fourVal.compareTo(twoVal)

const val plus1 = oneVal.plus(intVal)
const val plus2 = twoVal.plus(intVal)
const val plus3 = threeVal.plus(intVal)
const val plus4 = fourVal.plus(intVal)

const val minusChar1 = oneVal.minus(twoVal)
const val minusChar2 = twoVal.minus(twoVal)
const val minusChar3 = threeVal.minus(twoVal)
const val minusChar4 = fourVal.minus(twoVal)

const val minusInt1 = oneVal.minus(intVal)
const val minusInt2 = twoVal.minus(intVal)
const val minusInt3 = threeVal.minus(intVal)
const val minusInt4 = fourVal.minus(intVal)

const val convert1 = oneVal.toByte()
const val convert2 = oneVal.toChar()
const val convert3 = oneVal.toShort()
const val convert4 = oneVal.toInt()
const val convert5 = oneVal.toLong()
const val convert6 = oneVal.toFloat()
const val convert7 = oneVal.toDouble()

const val equals1 = oneVal == twoVal
const val equals2 = twoVal == twoVal
const val equals3 = threeVal == twoVal
const val equals4 = fourVal == twoVal

const val toString1 = oneVal.toString()
const val toString2 = twoVal.toString()

const val code1 = oneVal.code
const val code2 = twoVal.code
const val code3 = threeVal.code
const val code4 = fourVal.code

fun box(): String {
    if (compareTo1 != -1)   return "Fail 1.1"
    if (compareTo2 != 0)    return "Fail 1.2"
    if (compareTo3 != 1)    return "Fail 1.3"
    if (compareTo4 != 1)    return "Fail 1.4"

    if (plus1 != '6')   return "Fail 2.1"
    if (plus2 != '7')   return "Fail 2.2"
    if (plus3 != '8')   return "Fail 2.3"
    if (plus4 != '9')   return "Fail 2.4"

    if (minusChar1 != -1)   return "Fail 3.1"
    if (minusChar2 != 0)    return "Fail 3.2"
    if (minusChar3 != 1)    return "Fail 3.3"
    if (minusChar4 != 2)    return "Fail 3.4"

    if (minusInt1 != ',')   return "Fail 4.1"
    if (minusInt2 != '-')   return "Fail 4.2"
    if (minusInt3 != '.')   return "Fail 4.3"
    if (minusInt4 != '/')   return "Fail 4.4"

    if (convert1 != 49.toByte())    return "Fail 5.1"
    if (convert2 != '1')            return "Fail 5.2"
    if (convert3 != 49.toShort())   return "Fail 5.3"
    if (convert4 != 49)             return "Fail 5.4"
    if (convert5 != 49L)            return "Fail 5.5"
    if (convert6 != 49.0f)          return "Fail 5.6"
    if (convert7 != 49.0)           return "Fail 5.7"

    if (equals1 != false)   return "Fail 6.1"
    if (equals2 != true)    return "Fail 6.2"
    if (equals3 != false)   return "Fail 6.3"
    if (equals4 != false)   return "Fail 6.4"

    if (toString1 != "1")   return "Fail 7.1"
    if (toString2 != "2")   return "Fail 7.2"

    if (code1 != 49)   return "Fail 8.1"
    if (code2 != 50)   return "Fail 8.2"
    if (code3 != 51)   return "Fail 8.3"
    if (code4 != 52)   return "Fail 8.4"
    return "OK"
}