// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER
// KT-10036 Ambiguous overload cannot be resolved when using a member function reference in Beta 2, that worked in Beta 1

class OverloadTest {
    fun foo(bar: Boolean) {}
    fun foo(bar: Any?) {}
}

object Literal

inline fun <T : Any> OverloadTest.overload(value: T?, function: OverloadTest.(T) -> Unit) {
    if (value == null) foo(Literal) else function(<!DEBUG_INFO_SMARTCAST!>value<!>)
}

// Overload resolution ambiguity
fun OverloadTest.overloadBoolean(value: Boolean?) = overload(value, OverloadTest::foo)

// Works fine
fun OverloadTest.overloadBoolean2(value: Boolean?) = overload(value) { foo(it) }