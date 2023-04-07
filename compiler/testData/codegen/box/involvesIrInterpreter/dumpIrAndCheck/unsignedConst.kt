// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB

const val byteVal: UByte = <!EVALUATED("1")!>1u<!>
const val shortVal: UShort = <!EVALUATED("2")!>2u<!>
const val intVal: UInt = <!EVALUATED("3")!>3u<!>
const val longVal: ULong = <!EVALUATED("4")!>4uL<!>

fun box(): String {
    if (<!EVALUATED("1")!>byteVal<!> != 1u.toUByte())    return "Fail 1"
    if (<!EVALUATED("2")!>shortVal<!> != 2u.toUShort())  return "Fail 2"
    if (<!EVALUATED("3")!>intVal<!> != 3u.toUInt())      return "Fail 3"
    if (<!EVALUATED("4")!>longVal<!> != 4u.toULong())    return "Fail 4"
    return "OK"
}
