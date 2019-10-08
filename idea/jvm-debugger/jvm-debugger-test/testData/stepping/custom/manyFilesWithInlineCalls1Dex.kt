// EMULATE_DEX: true
// FILE: manyFilesWithInlineCalls1Dex.kt

package manyFilesWithInlineCalls1Dex

import manyFilesWithInlineCalls1Dex.first.*
import manyFilesWithInlineCalls1Dex.second.*

fun main(args: Array<String>) {
    firstInline()
}

fun unused() {
    secondInline()
}

// ADDITIONAL_BREAKPOINT: manyFilesWithInlineCalls1Dex.First.kt: Breakpoint 1

// FILE: manyFilesWithInlineCalls1Dex.First.kt
package manyFilesWithInlineCalls1Dex.first

inline fun firstInline() {
    // Breakpoint 1
    1 + 1
}

// FILE: manyFilesWithInlineCalls1Dex.Second.kt
package manyFilesWithInlineCalls1Dex.second

inline fun secondInline() {
    1 + 1
}
