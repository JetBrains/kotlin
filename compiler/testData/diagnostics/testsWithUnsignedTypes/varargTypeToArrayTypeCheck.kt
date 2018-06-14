// !LANGUAGE: +InlineClasses
// !SKIP_METADATA_VERSION_CHECK

fun ubyte(vararg a: UByte): UByteArray = a
fun ushort(vararg a: UShort): UShortArray = a
fun uint(vararg a: UInt): UIntArray = a
fun ulong(vararg a: ULong): ULongArray = a

fun rawUInt(vararg a: UInt): IntArray = <!TYPE_MISMATCH!>a<!>
