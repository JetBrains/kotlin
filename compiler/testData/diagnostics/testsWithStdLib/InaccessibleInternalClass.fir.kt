// FILE: a.kt
package p

class FilteringSequence

// FILE: b.kt
// SKIP_TXT
package kotlin.sequences

import p.*

interface I {
    val v1: FilteringSequence
    val v2: IndexingSequence<String>
}
