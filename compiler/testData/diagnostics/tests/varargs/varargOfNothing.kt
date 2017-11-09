// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testVarargOfNothing(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: Nothing) {}

fun testVarargOfNNothing(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: Nothing?) {}

fun <T : Nothing?> testVarargOfT(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: T) {}

fun outer() {
    fun testVarargOfNothing(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: Nothing) {}

    fun testVarargOfNNothing(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: Nothing?) {}

    fun <T : Nothing?> testVarargOfT(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> v: T) {}
}
