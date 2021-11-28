
fun ubyte(vararg a: <!OPT_IN_USAGE!>UByte<!>): <!OPT_IN_USAGE!>UByteArray<!> = <!OPT_IN_USAGE!>a<!>
fun ushort(vararg a: <!OPT_IN_USAGE!>UShort<!>): <!OPT_IN_USAGE!>UShortArray<!> = <!OPT_IN_USAGE!>a<!>
fun uint(vararg a: <!OPT_IN_USAGE!>UInt<!>): <!OPT_IN_USAGE!>UIntArray<!> = <!OPT_IN_USAGE!>a<!>
fun ulong(vararg a: <!OPT_IN_USAGE!>ULong<!>): <!OPT_IN_USAGE!>ULongArray<!> = <!OPT_IN_USAGE!>a<!>

fun rawUInt(vararg a: <!OPT_IN_USAGE!>UInt<!>): IntArray = <!OPT_IN_USAGE, RETURN_TYPE_MISMATCH!>a<!>
