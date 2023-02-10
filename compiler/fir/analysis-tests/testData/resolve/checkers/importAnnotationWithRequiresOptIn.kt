// ISSUE: KT-53671

// FILE: a.kt
@RequiresOptIn
annotation class MyOptIn

// FILE: b.kt
package vvv

import MyOptIn // OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN
