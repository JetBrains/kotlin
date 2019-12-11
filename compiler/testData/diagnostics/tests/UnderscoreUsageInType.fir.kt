// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

class `_`<`__`> {
    fun testTypeArgument(x: List<__>) = x
    fun testTypeArgument2(x: List<`__`>) = x
}

fun _<Any>.testTypeConstructor() {}
fun `_`<Any>.testTypeConstructor2() {}

val testConstructor = _<Any>()
val testConstructor2 = `_`<Any>()
