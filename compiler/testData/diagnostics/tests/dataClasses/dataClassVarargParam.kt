data class My(val x: Int, <!DATA_CLASS_VARARG_PARAMETER!>vararg val y: String<!>)

data class Your(<!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER!>vararg <!UNUSED_PARAMETER!>z<!>: String<!>)