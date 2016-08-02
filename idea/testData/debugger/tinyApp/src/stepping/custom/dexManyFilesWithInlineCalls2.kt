package dexManyFilesWithInlineCalls2

import dexManyFilesWithInlineCalls2.first.*
import dexManyFilesWithInlineCalls2.second.*

fun main(args: Array<String>) {
    secondInline()
}

fun unused() {
    firstInline()
}

// ADDITIONAL_BREAKPOINT: dexManyFilesWithInlineCalls2.Second.kt: Breakpoint 1