// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR

const val trueVal = true
const val falseVal = false

const val charOneVal = '1'
const val charTwoVal = '2'
const val charThreeVal = '3'
const val charFourVal = '4'

const val byteMinusOneVal = (-1).toByte()
const val byteOneVal = 1.toByte()
const val byteTwoVal = 2.toByte()
const val byteThreeVal = 3.toByte()
const val byteFourVal = 4.toByte()

const val shortMinusOneVal = (-1).toShort()
const val shortOneVal = 1.toShort()
const val shortTwoVal = 2.toShort()
const val shortThreeVal = 3.toShort()
const val shortFourVal = 4.toShort()

const val intMinusOneVal = -1
const val intOneVal = 1
const val intTwoVal = 2
const val intThreeVal = 3
const val intFourVal = 4

const val longMinusOneVal = -1L
const val longOneVal = 1L
const val longTwoVal = 2L
const val longThreeVal = 3L
const val longFourVal = 4L

const val floatMinusOneVal = -1.0f
const val floatOneVal = 1.0f
const val floatTwoVal = 2.0f
const val floatThreeVal = 3.0f
const val floatFourVal = 4.0f

const val doubleMinusOneVal = -1.0
const val doubleOneVal = 1.0
const val doubleTwoVal = 2.0
const val doubleThreeVal = 3.0
const val doubleFourVal = 4.0

const val someStr = "123"
const val otherStr = "other"

const val equalsBoolean1 = trueVal.equals(trueVal)
const val equalsBoolean2 = trueVal == falseVal
const val equalsBoolean3 = falseVal.equals(1)

const val equalsChar1 = charOneVal.equals(charTwoVal)
const val equalsChar2 = charTwoVal.equals(charTwoVal)
const val equalsChar3 = charThreeVal == charTwoVal
const val equalsChar4 = charFourVal.equals(1)

const val equalsByte1 = byteOneVal.equals(byteTwoVal)
const val equalsByte2 = byteTwoVal.equals(byteTwoVal)
const val equalsByte3 = byteThreeVal == byteTwoVal
const val equalsByte4 = byteFourVal.equals(1)

const val equalsShort1 = shortOneVal.equals(shortTwoVal)
const val equalsShort2 = shortTwoVal.equals(shortTwoVal)
const val equalsShort3 = shortThreeVal == shortTwoVal
const val equalsShort4 = shortFourVal.equals(1)

const val equalsInt1 = intOneVal.equals(intTwoVal)
const val equalsInt2 = intTwoVal.equals(intTwoVal)
const val equalsInt3 = intThreeVal == intTwoVal
const val equalsInt4 = intFourVal.equals(1)

const val equalsLong1 = longOneVal.equals(longTwoVal)
const val equalsLong2 = longTwoVal.equals(longTwoVal)
const val equalsLong3 = longThreeVal == longTwoVal
const val equalsLong4 = longFourVal.equals(1)

const val equalsFloat1 = floatOneVal.equals(floatTwoVal)
const val equalsFloat2 = floatTwoVal.equals(floatTwoVal)
const val equalsFloat3 = floatThreeVal == floatTwoVal
const val equalsFloat4 = floatFourVal.equals(1)

const val equalsDouble1 = doubleOneVal.equals(doubleTwoVal)
const val equalsDouble2 = doubleTwoVal.equals(doubleTwoVal)
const val equalsDouble3 = doubleThreeVal == doubleTwoVal
const val equalsDouble4 = doubleFourVal.equals(1)

const val equalsString1 = someStr.equals(otherStr)
const val equalsString2 = someStr.equals("123")
const val equalsString3 = otherStr == someStr
const val equalsString4 = someStr.equals(1)

fun box(): String {
    if (equalsBoolean1 != true)    return "Fail 1.1"
    if (equalsBoolean2 != false)   return "Fail 1.2"
    if (equalsBoolean3 != false)   return "Fail 1.3"

    if (equalsChar1 != false)   return "Fail 2.1"
    if (equalsChar2 != true)    return "Fail 2.2"
    if (equalsChar3 != false)   return "Fail 2.3"
    if (equalsChar4 != false)   return "Fail 2.3"

    if (equalsByte1 != false)   return "Fail 3.1"
    if (equalsByte2 != true)    return "Fail 3.2"
    if (equalsByte3 != false)   return "Fail 3.3"
    if (equalsByte4 != false)   return "Fail 3.3"

    if (equalsShort1 != false)   return "Fail 4.1"
    if (equalsShort2 != true)    return "Fail 4.2"
    if (equalsShort3 != false)   return "Fail 4.3"
    if (equalsShort4 != false)   return "Fail 4.3"

    if (equalsInt1 != false)   return "Fail 5.1"
    if (equalsInt2 != true)    return "Fail 5.2"
    if (equalsInt3 != false)   return "Fail 5.3"
    if (equalsInt4 != false)   return "Fail 5.3"

    if (equalsLong1 != false)   return "Fail 6.1"
    if (equalsLong2 != true)    return "Fail 6.2"
    if (equalsLong3 != false)   return "Fail 6.3"
    if (equalsLong4 != false)   return "Fail 6.3"

    if (equalsFloat1 != false)   return "Fail 7.1"
    if (equalsFloat2 != true)    return "Fail 7.2"
    if (equalsFloat3 != false)   return "Fail 7.3"
    if (equalsFloat4 != false)   return "Fail 7.3"

    if (equalsDouble1 != false)   return "Fail 8.1"
    if (equalsDouble2 != true)    return "Fail 8.2"
    if (equalsDouble3 != false)   return "Fail 8.3"
    if (equalsDouble4 != false)   return "Fail 8.3"

    if (equalsString1 != false)   return "Fail 9.1"
    if (equalsString2 != true)    return "Fail 9.2"
    if (equalsString3 != false)   return "Fail 9.3"
    if (equalsString4 != false)   return "Fail 9.3"

    return "OK"
}