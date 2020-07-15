// FILE: a.kt
package p

class FilteringSequence

// FILE: b.kt
// SKIP_TXT
package kotlin.sequences

import p.*

interface I {
    val <!EXPOSED_PROPERTY_TYPE!>v1<!>: FilteringSequence
    val <!EXPOSED_PROPERTY_TYPE!>v2<!>: IndexingSequence<String>
}
