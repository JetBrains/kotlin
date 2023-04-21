// WITH_STDLIB

@JvmInline
value class IcInt(val x: Int)

@JvmInline
value class IcIntN(val x: Int?)

@JvmInline
value class IcStr(val x: String)

@JvmInline
value class IcByteGeneric<T>(val x: Byte)

// OK checks:

fun checkBool(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Boolean><!>
fun checkByte(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Byte><!>
fun checkShort(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Short><!>
fun checkInt(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Int><!>
fun checkLong(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Long><!>
fun checkFloat(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Float><!>
fun checkDouble(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Double><!>
fun checkChar(p: Any) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<Char><!>

fun checkUByte(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UByte><!>
fun checkUShort(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UShort><!>
fun checkUInt(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UInt><!>
fun checkULong(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<ULong><!>


// Forbidden checks:

fun checkNoArg(p: Any?) = p is <!NO_TYPE_ARGUMENTS_ON_RHS!>VArray<!>
fun checkTwoArg(p: Any?) = p is VArray<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>

fun checkIcInt(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<IcInt><!>

fun checkUByteN(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UByte?><!>
fun checkUShortN(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UShort?><!>
fun checkUIntN(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<UInt?><!>
fun checkULongN(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<ULong?><!>

fun checkIcIntN(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<IcIntN><!>
fun checkStr(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<String><!>
fun checkIcStr(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<IcStr><!>
fun checkIcByteGenericStar(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<IcByteGeneric<*>><!>
fun checkIcByteGenericInt(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<IcByteGeneric<Int>><!>
fun check2DVArray(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<VArray<Int>><!>
fun checkIntIn(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<VArray<in Int>><!>
fun checkIntOut(p: Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<VArray<out Int>><!>
fun checkStar(p: Any?) = p is VArray<*>
fun <T> checkT(p:Any?) = p is <!CANNOT_CHECK_FOR_ERASED!>VArray<T><!>
