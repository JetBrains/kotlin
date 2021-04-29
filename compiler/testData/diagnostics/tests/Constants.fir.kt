// !CHECK_TYPE

fun varargByte(vararg v: Byte) = v

fun varargShort(vararg v: Short) = v

fun varargInt(vararg v: Int) = v

fun varargLong(vararg v: Long) = v

fun varargFloat(vararg v: Float) = v

fun varargDouble(vararg v: Double) = v

fun <T> testFun(p: T) {}

fun test() {
    checkSubtype<Byte>(1)
    checkSubtype<Short>(1)
    checkSubtype<Int>(1)
    checkSubtype<Long>(1)

    checkSubtype<Long>(0x001)
    checkSubtype<Int>(0b001)

    checkSubtype<Double>(0.1)
    checkSubtype<Float>(0.1.toFloat())

    checkSubtype<Double>(1e5)
    checkSubtype<Float>(1e-5.toFloat())

    checkSubtype<Double>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    checkSubtype<Float>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)

    1 as Byte
    1 <!USELESS_CAST!>as Int<!>
    0xff as Long

    1.1 as Int
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>1.1<!>)

    varargByte(0x77, 1, 3, <!ARGUMENT_TYPE_MISMATCH!>200<!>, 0b111)
    varargShort(0x777, 1, 2, 3, <!ARGUMENT_TYPE_MISMATCH!>200000<!>, 0b111)
    varargInt(0x77777777, <!ARGUMENT_TYPE_MISMATCH!>0x7777777777<!>, 1, 2, 3, 2000000000, 0b111)
    varargLong(0x777777777777, 1, 2, 3, 200000, 0b111)
    varargFloat(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>1.0<!>, <!ARGUMENT_TYPE_MISMATCH!>-0.1<!>, <!ARGUMENT_TYPE_MISMATCH!>1e4<!>, <!ARGUMENT_TYPE_MISMATCH!>1e-4<!>, <!ARGUMENT_TYPE_MISMATCH!>-1e4<!>)
    varargDouble(<!ARGUMENT_TYPE_MISMATCH!>1<!>, 1.0, -0.1, 1e4, 1e-4, -1e4)

    testFun(1.0)
    testFun<Float>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)
    testFun(1.0.toFloat())
    testFun<Float>(1.0.toFloat())
}
