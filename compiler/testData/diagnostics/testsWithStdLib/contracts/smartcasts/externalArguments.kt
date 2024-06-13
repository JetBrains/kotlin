// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.reflect.KProperty

fun testLambdaArgumentSmartCast(foo: Int?) {
    val v = run {
        if (foo != null)
            return@run <!DEBUG_INFO_SMARTCAST!>foo<!>
        15
    }
}

class D {
    operator fun getValue(ref: Any?, property: KProperty<*>): Int = 42
}

fun testSmartCastInDelegate(d: D?) {
    if (d == null) return
    val v: Int by <!DEBUG_INFO_SMARTCAST!>d<!>
}

fun testFunctionCallSmartcast(fn: (() -> Unit)?) {
    if (fn == null) return

    <!DEBUG_INFO_SMARTCAST!>fn<!>()
}

fun testCallableRefernceSmartCast() {
    fun forReference() {}

    val refernece = if (true) ::forReference else null
    if (refernece == null)
        return

    <!DEBUG_INFO_SMARTCAST!>refernece<!>()
}
