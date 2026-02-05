fun <T> T.id() = this

const val minusOneVal = -1.0f
const val oneVal = 1.0f
const val twoVal = 2.0f
const val threeVal = 3.0f
const val fourVal = 4.0f
const val oneAndAHalf = 1.5f

const val byteVal = 2.toByte()
const val shortVal = 2.toShort()
const val intVal = 2
const val longVal = 2L
const val floatVal = 2.0f
const val doubleVal = 2.0

const val funCompareTo1 = oneVal.compareTo(twoVal)
const val funCompareTo2 = twoVal.compareTo(twoVal)
const val funCompareTo3 = threeVal.compareTo(twoVal)
const val funCompareTo4 = twoVal.compareTo(byteVal)
const val funCompareTo5 = twoVal.compareTo(shortVal)
const val funCompareTo6 = twoVal.compareTo(intVal)
const val funCompareTo7 = twoVal.compareTo(longVal)
const val funCompareTo8 = twoVal.compareTo(doubleVal)

const val singCompareTo1 = oneVal >= twoVal
const val singCompareTo2 = twoVal > twoVal
const val singCompareTo3 = threeVal < twoVal
const val singCompareTo4 = twoVal <= byteVal

const val plus1 = oneVal.plus(twoVal)
const val plus2 = twoVal.plus(twoVal)
const val plus3 = threeVal.plus(twoVal)
const val plus4 = twoVal.plus(byteVal)
const val plus5 = twoVal.plus(shortVal)
const val plus6 = twoVal.plus(intVal)
const val plus7 = twoVal.plus(longVal)
const val plus8 = twoVal.plus(doubleVal)

const val minus1 = oneVal.minus(twoVal)
const val minus2 = twoVal.minus(twoVal)
const val minus3 = threeVal.minus(twoVal)
const val minus4 = twoVal.minus(byteVal)
const val minus5 = twoVal.minus(shortVal)
const val minus6 = twoVal.minus(intVal)
const val minus7 = twoVal.minus(longVal)
const val minus8 = twoVal.minus(doubleVal)

const val times1 = oneVal.times(twoVal)
const val times2 = twoVal.times(twoVal)
const val times3 = threeVal.times(twoVal)
const val times4 = twoVal.times(byteVal)
const val times5 = twoVal.times(shortVal)
const val times6 = twoVal.times(intVal)
const val times7 = twoVal.times(longVal)
const val times8 = twoVal.times(doubleVal)

const val div1 = oneVal.div(twoVal)
const val div2 = twoVal.div(twoVal)
const val div3 = threeVal.div(twoVal)
const val div4 = twoVal.div(byteVal)
const val div5 = twoVal.div(shortVal)
const val div6 = twoVal.div(intVal)
const val div7 = twoVal.div(longVal)
const val div8 = twoVal.div(doubleVal)

const val rem1 = oneVal.rem(twoVal)
const val rem2 = twoVal.rem(twoVal)
const val rem3 = threeVal.rem(twoVal)
const val rem4 = twoVal.rem(byteVal)
const val rem5 = twoVal.rem(shortVal)
const val rem6 = twoVal.rem(intVal)
const val rem7 = twoVal.rem(longVal)
const val rem8 = twoVal.rem(doubleVal)

const val unaryPlus1 = oneVal.unaryPlus()
const val unaryPlus2 = minusOneVal.unaryPlus()
const val unaryMinus1 = oneVal.unaryMinus()
const val unaryMinus2 = minusOneVal.unaryMinus()

const val convert1 = oneVal.toInt().toChar()
const val convert2 = oneVal.toInt()
const val convert3 = oneVal.toLong()
const val convert4 = oneVal.toFloat()
const val convert5 = oneVal.toDouble()

const val equals1 = oneVal == twoVal
const val equals2 = twoVal == twoVal
const val equals3 = threeVal == twoVal
const val equals4 = fourVal == twoVal

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (funCompareTo1.id() != -1)   return "Fail 1.1"
    if (funCompareTo2.id() != 0)    return "Fail 1.2"
    if (funCompareTo3.id() != 1)    return "Fail 1.3"
    if (funCompareTo4.id() != 0)    return "Fail 1.4"
    if (funCompareTo5.id() != 0)    return "Fail 1.5"
    if (funCompareTo6.id() != 0)    return "Fail 1.6"
    if (funCompareTo7.id() != 0)    return "Fail 1.7"
    if (funCompareTo8.id() != 0)    return "Fail 1.8"

    if (singCompareTo1.id() != false)    return "Fail 1.9"
    if (singCompareTo2.id() != false)    return "Fail 1.10"
    if (singCompareTo3.id() != false)    return "Fail 1.11"
    if (singCompareTo4.id() != true)    return "Fail 1.12"

    if (plus1.id() != 3f)     return "Fail 2.1"
    if (plus2.id() != 4f)     return "Fail 2.2"
    if (plus3.id() != 5f)     return "Fail 2.3"
    if (plus4.id() != 4f)     return "Fail 2.4"
    if (plus5.id() != 4f)     return "Fail 2.5"
    if (plus6.id() != 4f)     return "Fail 2.6"
    if (plus7.id() != 4.0f)   return "Fail 2.7"
    if (plus8.id() != 4.0)    return "Fail 2.8"

    if (minus1.id() != -1f)     return "Fail 3.1"
    if (minus2.id() != 0f)      return "Fail 3.2"
    if (minus3.id() != 1f)      return "Fail 3.3"
    if (minus4.id() != 0f)      return "Fail 3.4"
    if (minus5.id() != 0f)      return "Fail 3.5"
    if (minus6.id() != 0f)      return "Fail 3.6"
    if (minus7.id() != 0.0f)    return "Fail 3.7"
    if (minus8.id() != 0.0)     return "Fail 3.8"

    if (times1.id() != 2f)      return "Fail 4.1"
    if (times2.id() != 4f)      return "Fail 4.2"
    if (times3.id() != 6f)      return "Fail 4.3"
    if (times4.id() != 4f)      return "Fail 4.4"
    if (times5.id() != 4f)      return "Fail 4.5"
    if (times6.id() != 4f)      return "Fail 4.6"
    if (times7.id() != 4.0f)    return "Fail 4.7"
    if (times8.id() != 4.0)     return "Fail 4.8"

    if (div1.id() != 0.5f)      return "Fail 5.1"
    if (div2.id() != 1.0f)      return "Fail 5.2"
    if (div3.id() != 1.5f)      return "Fail 5.3"
    if (div4.id() != 1f)        return "Fail 5.4"
    if (div5.id() != 1f)        return "Fail 5.5"
    if (div6.id() != 1f)        return "Fail 5.6"
    if (div7.id() != 1.0f)      return "Fail 5.7"
    if (div8.id() != 1.0)       return "Fail 5.8"

    if (rem1.id() != 1f)      return "Fail 6.1"
    if (rem2.id() != 0f)      return "Fail 6.2"
    if (rem3.id() != 1f)      return "Fail 6.3"
    if (rem4.id() != 0f)      return "Fail 6.4"
    if (rem5.id() != 0f)      return "Fail 6.5"
    if (rem6.id() != 0f)      return "Fail 6.6"
    if (rem7.id() != 0.0f)    return "Fail 6.7"
    if (rem8.id() != 0.0)     return "Fail 6.8"

    if (unaryPlus1.id() != 1f)    return "Fail 7.1"
    if (unaryPlus2.id() != -1f)   return "Fail 7.2"
    if (unaryMinus1.id() != -1f)  return "Fail 7.3"
    if (unaryMinus2.id() != 1f)   return "Fail 7.4"

    if (convert1.id() != '')  return "Fail 8.1"
    if (convert2.id() != 1)      return "Fail 8.2"
    if (convert3.id() != 1L)     return "Fail 8.3"
    if (convert4.id() != 1.0f)   return "Fail 8.4"
    if (convert5.id() != 1.0)    return "Fail 8.5"

    if (equals1.id() != false)   return "Fail 9.1"
    if (equals2.id() != true)    return "Fail 9.2"
    if (equals3.id() != false)   return "Fail 9.3"
    if (equals4.id() != false)   return "Fail 9.4"

    return "OK"
}
