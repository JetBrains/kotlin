// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.<!UNRESOLVED_REFERENCE!>browser<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>window<!>
import <!UNRESOLVED_REFERENCE!>kotlinx<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>cinterop<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>CPointed<!>

fun jvmSpecific(args: Array<String>) {
    val x: <!UNRESOLVED_REFERENCE!>Cloneable<!>? = null
    args.<!UNRESOLVED_REFERENCE!>clone<!>()
}

fun nativeSpecific() {
    val x: <!UNRESOLVED_REFERENCE!>CPointed<!>? = null
}

fun jsSpecific() {
    val windowClosed = <!UNRESOLVED_REFERENCE!>window<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>closed<!>
}