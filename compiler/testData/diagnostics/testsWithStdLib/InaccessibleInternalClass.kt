// FILE: a.kt
package p

class FilteringSequence

// FILE: b.kt
// SKIP_TXT
package kotlin.sequences

import p.*

interface I {
    val v1: FilteringSequence
    <!EXPOSED_PROPERTY_TYPE!>val v2: <!INVISIBLE_REFERENCE!>IndexingSequence<!><String><!>
}
