// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

fun isBoolVArray(p: Any?) = p is VArray<Boolean>
fun isByteVArray(p: Any?) = p is VArray<Byte>
fun isShortVArray(p: Any?) = p is VArray<Short>
fun isIntVArray(p: Any?) = p is VArray<Int>
fun isLongVArray(p: Any?) = p is VArray<Long>
fun isFloatVArray(p: Any?) = p is VArray<Float>
fun isDoubleVArray(p: Any?) = p is VArray<Double>
fun isCharVArray(p: Any?) = p is VArray<Char>
fun isUByteVArray(p: Any?) = p is VArray<UByte>
fun isUShortVArray(p: Any?) = p is VArray<UShort>
fun isUIntVArray(p: Any?) = p is VArray<UInt>
fun isULongVArray(p: Any?) = p is VArray<ULong>


enum class VArrayType { BOOL, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, OTHER}

val typeToСheckingFuncs = mapOf(
    VArrayType.BOOL to listOf(::isBoolVArray),
    VArrayType.BYTE to listOf(::isByteVArray, ::isUByteVArray),
    VArrayType.SHORT to listOf(::isShortVArray, ::isUShortVArray),
    VArrayType.INT to listOf(::isIntVArray, ::isUIntVArray),
    VArrayType.LONG to listOf(::isLongVArray, ::isULongVArray),
    VArrayType.FLOAT to listOf(::isFloatVArray),
    VArrayType.DOUBLE to listOf(::isDoubleVArray),
    VArrayType.CHAR to listOf(::isCharVArray),
)

fun isTypeExpected(obj: Any?, expectedType: VArrayType): Boolean {
    typeToСheckingFuncs.forEach { (type, checkingFuncs) ->
        if (type == expectedType && !checkingFuncs.all { it(obj) }) return false
        if (type != expectedType && checkingFuncs.any { it(obj) }) return false
    }
    return true
}

@JvmInline
value class IcInt(val x: Int)

@JvmInline
value class IcIcInt(val x: IcInt)

@JvmInline
value class IcStr(val x: String)


fun box(): String {

    val boolVArray = VArray<Boolean>(1) { true }
    val byteVArray = VArray<Byte>(1) { 0.toByte() }
    val shortVArray = VArray<Short>(1) { 0.toShort() }
    val intVArray = VArray<Int>(1) { 0 }
    val longVArray = VArray<Long>(1) { 0.toLong() }
    val floatVArray = VArray<Float>(1) { 0.0.toFloat() }
    val doubleVArray = VArray<Double>(1) { 0.0 }
    val charVArray = VArray<Char>(1) { 'a' }


    val strVArray = VArray<String>(1) { "a" }
    val icIntVArray = VArray<IcInt>(1) { IcInt(42) }
    val icIcIntVArray = VArray<IcIcInt>(1) { IcIcInt(IcInt(42)) }
    val icStrVArray = VArray<IcStr>(1) { IcStr("a") }
    val double3dVArray = VArray(1) { VArray(1) { VArray(1) { 0.0 } } }
    val str2dVArray = VArray(1) { VArray(1) { "a" } }

    val uByteVArray = VArray<UByte>(1) { 0.toUByte() }
    val uShortVArray = VArray<UShort>(1) { 0.toUShort() }
    val uIntVArray = VArray<UInt>(1) { 0.toUInt() }
    val uLongVArray = VArray<ULong>(1) { 0.toULong() }

    val arrayOfInt = Array<Int>(1) { 0 }
    val arrayOfStr = Array<String>(1) { "a" }
    val arrayOfIcInt = Array<IcInt>(1) { IcInt(0) }


    if (!isTypeExpected(boolVArray, VArrayType.BOOL)) return "Fail 1"
    if (!isTypeExpected(byteVArray, VArrayType.BYTE)) return "Fail 2"
    if (!isTypeExpected(shortVArray, VArrayType.SHORT)) return "Fail 3"
    if (!isTypeExpected(intVArray, VArrayType.INT)) return "Fail 4"
    if (!isTypeExpected(longVArray, VArrayType.LONG)) return "Fail 5"
    if (!isTypeExpected(floatVArray, VArrayType.FLOAT)) return "Fail 6"
    if (!isTypeExpected(doubleVArray, VArrayType.DOUBLE)) return "Fail 7"
    if (!isTypeExpected(charVArray, VArrayType.CHAR)) return "Fail 8"

    if (!isTypeExpected(icIntVArray, VArrayType.INT)) return "Fail 9"
    if (!isTypeExpected(strVArray, VArrayType.OTHER)) return "Fail 10"
    if (!isTypeExpected(icIcIntVArray, VArrayType.INT)) return "Fail 11"
    if (!isTypeExpected(icStrVArray, VArrayType.OTHER)) return "Fail 12"
    if (!isTypeExpected(double3dVArray, VArrayType.OTHER)) return "Fail 13"
    if (!isTypeExpected(str2dVArray, VArrayType.OTHER)) return "Fail 14"

    if (!isTypeExpected(uByteVArray, VArrayType.BYTE)) return "Fail 15"
    if (!isTypeExpected(uShortVArray, VArrayType.SHORT)) return "Fail 16"
    if (!isTypeExpected(uIntVArray, VArrayType.INT)) return "Fail 17"
    if (!isTypeExpected(uLongVArray, VArrayType.LONG)) return "Fail 18"

    if (!isTypeExpected(arrayOfInt, VArrayType.OTHER)) return "Fail 15"
    if (!isTypeExpected(arrayOfStr, VArrayType.OTHER)) return "Fail 15"
    if (!isTypeExpected(arrayOfIcInt, VArrayType.OTHER)) return "Fail 15"

    return "OK"
}

