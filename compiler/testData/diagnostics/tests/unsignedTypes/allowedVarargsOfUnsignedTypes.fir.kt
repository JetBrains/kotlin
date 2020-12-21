// !DIAGNOSTICS: -UNUSED_PARAMETER

fun ubyte(vararg a: UByte) {}
fun ushort(vararg a: UShort) {}
fun uint(vararg a: UInt) {}
fun ulong(vararg a: ULong) {}

class ValueParam(vararg val a: ULong)

annotation class Ann(vararg val a: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>UInt<!>)

fun array(vararg a: UIntArray) {}
