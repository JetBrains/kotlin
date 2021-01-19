// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlinx.browser.window
import kotlinx.<!UNRESOLVED_REFERENCE!>cinterop<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>CPointed<!>

fun jvmSpecific(args: Array<String>) {
    val x: <!UNRESOLVED_REFERENCE!>Cloneable<!>? = null
    args.<!UNRESOLVED_REFERENCE!>clone<!>()
}

fun nativeSpecific() {
    val x: <!UNRESOLVED_REFERENCE!>CPointed<!>? = null
}

fun jsSpecific() {
    val windowClosed = window.closed
}