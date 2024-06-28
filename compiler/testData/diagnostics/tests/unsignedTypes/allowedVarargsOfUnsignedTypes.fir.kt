// DIAGNOSTICS: -UNUSED_PARAMETER

fun ubyte(vararg a: <!OPT_IN_USAGE!>UByte<!>) {}
fun ushort(vararg a: <!OPT_IN_USAGE!>UShort<!>) {}
fun uint(vararg a: <!OPT_IN_USAGE!>UInt<!>) {}
fun ulong(vararg a: <!OPT_IN_USAGE!>ULong<!>) {}

class ValueParam(vararg val a: <!OPT_IN_USAGE!>ULong<!>)

annotation class Ann(vararg val a: <!OPT_IN_USAGE!>UInt<!>)

fun array(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> a: <!OPT_IN_USAGE!>UIntArray<!>) {}
