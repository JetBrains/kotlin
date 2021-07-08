// !DIAGNOSTICS: -UNUSED_PARAMETER

fun ubyte(vararg a: <!EXPERIMENTAL_API_USAGE!>UByte<!>) {}
fun ushort(vararg a: <!EXPERIMENTAL_API_USAGE!>UShort<!>) {}
fun uint(vararg a: <!EXPERIMENTAL_API_USAGE!>UInt<!>) {}
fun ulong(vararg a: <!EXPERIMENTAL_API_USAGE!>ULong<!>) {}

class ValueParam(vararg val a: <!EXPERIMENTAL_API_USAGE!>ULong<!>)

annotation class Ann(vararg val a: <!EXPERIMENTAL_API_USAGE!>UInt<!>)

fun array(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> a: <!EXPERIMENTAL_API_USAGE!>UIntArray<!>) {}
