// ISSUE: KT-73909
// WITH_EXTRA_CHECKERS
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlin.concurrent.*

fun foo(): AtomicInt? = null

private fun fooImpl(): AtomicInt? = null

fun AtomicLong.bar() {}

class Owner(<!UNUSED_PARAMETER!>l<!>: AtomicLong) {
    val AtomicReference<*>.r: Any? get() = null

    fun baz(arg: <!OPT_IN_USAGE_ERROR!>AtomicArray<!><*>) = <!OPT_IN_USAGE_ERROR!>arg<!>

    internal fun bazImpl(arg: AtomicLong) = arg

    var ai: AtomicInt? = null
        set(<!UNUSED_PARAMETER!>arg<!>: AtomicInt?) {}
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(c: <!DEBUG_INFO_MISSING_UNRESOLVED!>AtomicIntArray<!>)<!>
fun withContext() {
    val <!UNUSED_VARIABLE!>i<!>: AtomicInt? = null

    val <!UNUSED_VARIABLE!>f<!> = fun(<!UNUSED_ANONYMOUS_PARAMETER!>arg<!>: AtomicLong?): AtomicLong? = null

    val <!UNUSED_VARIABLE!>l<!> = { arg: <!OPT_IN_USAGE_ERROR!>AtomicIntArray<!> -> <!OPT_IN_USAGE_ERROR!>arg<!> }
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(c: <!DEBUG_INFO_MISSING_UNRESOLVED!>AtomicLongArray<!>)<!>
val some: Int get() = 0
