// FILE: a.kt
package p

class FilteringSequence

// FILE: b.kt
// SKIP_TXT
package kotlin.sequences

import p.*

interface I {
    val v1: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>FilteringSequence<!>
    val <!EXPOSED_PROPERTY_TYPE!>v2<!>: IndexingSequence<String>
}
