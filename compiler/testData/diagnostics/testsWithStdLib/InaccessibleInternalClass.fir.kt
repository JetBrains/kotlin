// FILE: a.kt
package p

class FilteringSequence

// FILE: b.kt
// SKIP_TXT
package kotlin.sequences

import p.*

interface I {
    <!EXPOSED_PROPERTY_TYPE!>val v1: FilteringSequence<!>
    <!EXPOSED_PROPERTY_TYPE!>val v2: IndexingSequence<String><!>
}
