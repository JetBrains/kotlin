// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testVarargOfNothing(vararg v: Nothing) {}

fun testVarargOfNNothing(vararg v: Nothing?) {}

fun <T : Nothing?> testVarargOfT(vararg v: T) {}

fun outer() {
    fun testVarargOfNothing(vararg v: Nothing) {}

    fun testVarargOfNNothing(vararg v: Nothing?) {}

    fun <T : Nothing?> testVarargOfT(vararg v: T) {}
}
