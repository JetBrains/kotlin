package dexManyFilesWithInlineCalls1

import dexManyFilesWithInlineCalls1.first.*
import dexManyFilesWithInlineCalls1.second.*

fun main(args: Array<String>) {
    firstInline()
}

fun unused() {
    secondInline()
}

// ADDITIONAL_BREAKPOINT: dexManyFilesWithInlineCalls1.First.kt: Breakpoint 1