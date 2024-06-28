// KT-59582

// FILE: a.kt
@RequiresOptIn
annotation class Ann()

// FILE: b.kt
package b

import <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>Ann<!>
