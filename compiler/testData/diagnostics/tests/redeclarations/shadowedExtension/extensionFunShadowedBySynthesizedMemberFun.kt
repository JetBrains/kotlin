// !DIAGNOSTICS: -UNUSED_PARAMETER

data class DataClass(val x: Int)

fun DataClass.<!EXTENSION_SHADOWED_BY_MEMBER!>component1<!>() = 42