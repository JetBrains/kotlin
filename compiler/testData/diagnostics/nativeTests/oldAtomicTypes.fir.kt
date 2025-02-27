// ISSUE: KT-73909
// WITH_EXTRA_CHECKERS
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlin.concurrent.*

fun foo(): <!NATIVE_SPECIFIC_ATOMIC!>AtomicInt?<!> = null

private fun fooImpl(): AtomicInt? = null

fun <!NATIVE_SPECIFIC_ATOMIC!>AtomicLong<!>.bar() {}

class Owner(l: <!NATIVE_SPECIFIC_ATOMIC!>AtomicLong<!>) {
    val <!NATIVE_SPECIFIC_ATOMIC!>AtomicReference<*><!>.r: Any? get() = null

    fun baz(arg: <!NATIVE_SPECIFIC_ATOMIC, OPT_IN_USAGE_ERROR!>AtomicArray<*><!>) = <!OPT_IN_USAGE_ERROR!>arg<!>

    internal fun bazImpl(arg: AtomicLong) = arg

    var ai: <!NATIVE_SPECIFIC_ATOMIC!>AtomicInt?<!> = null
        set(arg: <!REDUNDANT_SETTER_PARAMETER_TYPE!>AtomicInt?<!>) {}
}

context(c: <!NATIVE_SPECIFIC_ATOMIC, OPT_IN_USAGE_ERROR!>AtomicIntArray<!>)
fun withContext() {
    val <!UNUSED_VARIABLE!>i<!>: AtomicInt? = null

    val <!UNUSED_VARIABLE!>f<!> = fun(arg: AtomicLong?): AtomicLong? = null

    val <!UNUSED_VARIABLE!>l<!> = { arg: <!OPT_IN_USAGE_ERROR!>AtomicIntArray<!> -> <!OPT_IN_USAGE_ERROR!>arg<!> }
}

context(c: <!NATIVE_SPECIFIC_ATOMIC, OPT_IN_USAGE_ERROR!>AtomicLongArray<!>)
val some: Int get() = 0
