// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.browser.window
import <!UNRESOLVED_REFERENCE("kotlinx")!>kotlinx<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>cinterop<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>CPointed<!>

fun jvmSpecific() {
    val x: <!UNRESOLVED_REFERENCE("Cloneable")!>Cloneable<!>? = null
}

fun nativeSpecific() {
    val x: <!UNRESOLVED_REFERENCE("CPointed")!>CPointed<!>? = null
}

fun jsSpecific() {
    val windowClosed = window.closed
}