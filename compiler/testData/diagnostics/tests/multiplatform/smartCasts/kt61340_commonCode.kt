// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// MODULE: m1-common
// FILE: common.kt

expect val <!REDECLARATION!>foo<!>: Any

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Bar<!>() {
    val bus: Any
}

<!CONFLICTING_OVERLOADS!>fun common()<!> {
    if (<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> is String) <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!>

    val bar = Bar()
    if (bar.bus is String) <!SMARTCAST_IMPOSSIBLE{JVM}!>bar.bus<!>.length
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual val foo: Any = 2

actual class Bar actual constructor() {
    actual val bus: Any
        get() = "bus"
}
