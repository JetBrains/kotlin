// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: NATIVE

const val minusOneVal = -1L
const val oneVal = 1L
const val twoVal = 2L
const val threeVal = 3L
const val fourVal = 4L

const val byteVal = 2.toByte()
const val shortVal = 2.toShort()
const val intVal = 2
const val longVal = 2L
const val floatVal = 2.0f
const val doubleVal = 2.0

const val compareTo1 = oneVal.compareTo(twoVal)
const val compareTo2 = twoVal.compareTo(twoVal)
const val compareTo3 = threeVal.compareTo(twoVal)
const val compareTo4 = twoVal.compareTo(byteVal)
const val compareTo5 = twoVal.compareTo(shortVal)
const val compareTo6 = twoVal.compareTo(intVal)
const val compareTo7 = twoVal.compareTo(floatVal)
const val compareTo8 = twoVal.compareTo(doubleVal)

const val plus1 = oneVal.plus(twoVal)
const val plus2 = twoVal.plus(twoVal)
const val plus3 = threeVal.plus(twoVal)
const val plus4 = twoVal.plus(byteVal)
const val plus5 = twoVal.plus(shortVal)
const val plus6 = twoVal.plus(intVal)
const val plus7 = twoVal.plus(floatVal)
const val plus8 = twoVal.plus(doubleVal)

const val minus1 = oneVal.minus(twoVal)
const val minus2 = twoVal.minus(twoVal)
const val minus3 = threeVal.minus(twoVal)
const val minus4 = twoVal.minus(byteVal)
const val minus5 = twoVal.minus(shortVal)
const val minus6 = twoVal.minus(intVal)
const val minus7 = twoVal.minus(floatVal)
const val minus8 = twoVal.minus(doubleVal)

const val times1 = oneVal.times(twoVal)
const val times2 = twoVal.times(twoVal)
const val times3 = threeVal.times(twoVal)
const val times4 = twoVal.times(byteVal)
const val times5 = twoVal.times(shortVal)
const val times6 = twoVal.times(intVal)
const val times7 = twoVal.times(floatVal)
const val times8 = twoVal.times(doubleVal)

const val div1 = oneVal.div(twoVal)
const val div2 = twoVal.div(twoVal)
const val div3 = threeVal.div(twoVal)
const val div4 = twoVal.div(byteVal)
const val div5 = twoVal.div(shortVal)
const val div6 = twoVal.div(intVal)
const val div7 = twoVal.div(floatVal)
const val div8 = twoVal.div(doubleVal)

const val rem1 = oneVal.rem(twoVal)
const val rem2 = twoVal.rem(twoVal)
const val rem3 = threeVal.rem(twoVal)
const val rem4 = twoVal.rem(byteVal)
const val rem5 = twoVal.rem(shortVal)
const val rem6 = twoVal.rem(intVal)
const val rem7 = twoVal.rem(floatVal)
const val rem8 = twoVal.rem(doubleVal)

const val unaryPlus1 = oneVal.unaryPlus()
const val unaryPlus2 = minusOneVal.unaryPlus()
const val unaryMinus1 = oneVal.unaryMinus()
const val unaryMinus2 = minusOneVal.unaryMinus()

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

fun box(): String {
    if (compareTo1 != -1)   return "Fail 1.1"
    if (compareTo2 != 0)    return "Fail 1.2"
    if (compareTo3 != 1)    return "Fail 1.3"
    if (compareTo4 != 0)    return "Fail 1.4"
    if (compareTo5 != 0)    return "Fail 1.5"
    if (compareTo6 != 0)    return "Fail 1.6"
    if (compareTo7 != 0)    return "Fail 1.7"
    if (compareTo8 != 0)    return "Fail 1.8"

    if (plus1 != 3L)     return "Fail 2.1"
    if (plus2 != 4L)     return "Fail 2.2"
    if (plus3 != 5L)     return "Fail 2.3"
    if (plus4 != 4L)     return "Fail 2.4"
    if (plus5 != 4L)     return "Fail 2.5"
    if (plus6 != 4L)     return "Fail 2.6"
    if (plus7 != 4.0f)  return "Fail 2.7"
    if (plus8 != 4.0)   return "Fail 2.8"

    if (minus1 != -1L)       return "Fail 3.1"
    if (minus2 != 0L)        return "Fail 3.2"
    if (minus3 != 1L)        return "Fail 3.3"
    if (minus4 != 0L)        return "Fail 3.4"
    if (minus5 != 0L)        return "Fail 3.5"
    if (minus6 != 0L)        return "Fail 3.6"
    if (minus7 != 0.0f)     return "Fail 3.7"
    if (minus8 != 0.0)      return "Fail 3.8"

    if (times1 != 2L)        return "Fail 4.1"
    if (times2 != 4L)        return "Fail 4.2"
    if (times3 != 6L)        return "Fail 4.3"
    if (times4 != 4L)        return "Fail 4.4"
    if (times5 != 4L)        return "Fail 4.5"
    if (times6 != 4L)        return "Fail 4.6"
    if (times7 != 4.0f)     return "Fail 4.7"
    if (times8 != 4.0)      return "Fail 4.8"

    if (div1 != 0L)          return "Fail 5.1"
    if (div2 != 1L)          return "Fail 5.2"
    if (div3 != 1L)          return "Fail 5.3"
    if (div4 != 1L)          return "Fail 5.4"
    if (div5 != 1L)          return "Fail 5.5"
    if (div6 != 1L)          return "Fail 5.6"
    if (div7 != 1.0f)       return "Fail 5.7"
    if (div8 != 1.0)        return "Fail 5.8"

    if (rem1 != 1L)      return "Fail 6.1"
    if (rem2 != 0L)      return "Fail 6.2"
    if (rem3 != 1L)      return "Fail 6.3"
    if (rem4 != 0L)      return "Fail 6.4"
    if (rem5 != 0L)      return "Fail 6.5"
    if (rem6 != 0L)      return "Fail 6.6"
    if (rem7 != 0.0f)   return "Fail 6.7"
    if (rem8 != 0.0)    return "Fail 6.8"

    if (unaryPlus1 != 1L)    return "Fail 7.1"
    if (unaryPlus2 != -1L)   return "Fail 7.2"
    if (unaryMinus1 != -1L)  return "Fail 7.3"
    if (unaryMinus2 != 1L)   return "Fail 7.4"

    if (convert1 != 1.toByte())      return "Fail 8.1"
    if (convert2 != '')  return "Fail 8.2"
    if (convert3 != 1.toShort())      return "Fail 8.3"
    if (convert4 != 1)      return "Fail 8.4"
    if (convert5 != 1L)      return "Fail 8.5"
    if (convert6 != 1.0f)   return "Fail 8.6"
    if (convert7 != 1.0)    return "Fail 8.7"

    if (equals1 != false)   return "Fail 9.1"
    if (equals2 != true)    return "Fail 9.2"
    if (equals3 != false)   return "Fail 9.3"
    if (equals4 != false)   return "Fail 9.4"

    if (toString1 != "1")   return "Fail 10.1"
    if (toString2 != "2")   return "Fail 10.2"

    return "OK"
}