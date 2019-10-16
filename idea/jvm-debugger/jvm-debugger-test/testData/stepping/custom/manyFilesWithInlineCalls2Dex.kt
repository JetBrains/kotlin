// EMULATE_DEX: true
// FILE: manyFilesWithInlineCalls2Dex.kt

package manyFilesWithInlineCalls2Dex

import manyFilesWithInlineCalls2Dex.first.*
import manyFilesWithInlineCalls2Dex.second.*

fun main(args: Array<String>) {
    secondInline()
}

fun unused() {
    firstInline()
}

// ADDITIONAL_BREAKPOINT: manyFilesWithInlineCalls2Dex.Second.kt: Breakpoint 1

// FILE: manyFilesWithInlineCalls2Dex.First.kt
package manyFilesWithInlineCalls2Dex.first

inline fun firstInline() {
    1 + 1
}

// FILE: manyFilesWithInlineCalls2Dex.Second.kt
package manyFilesWithInlineCalls2Dex.second

inline fun secondInline() {
    // Breakpoint 1
    1 + 1
}