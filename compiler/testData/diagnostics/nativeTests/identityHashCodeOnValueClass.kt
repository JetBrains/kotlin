// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

inline value class VC(val s: String)
value class FVC(val s: String)

@OptIn(ExperimentalNativeApi::class)
fun test(p: VC, p2: VC?, p3: Int, p4: Int?, p5: FVC, p6: FVC?) {
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p<!>.identityHashCode()
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p2<!>.identityHashCode()
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p3<!>.identityHashCode()
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p4<!>.identityHashCode()
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p5<!>.identityHashCode()
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p6<!>.identityHashCode()

    with(p) { <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>identityHashCode()<!> }
}
