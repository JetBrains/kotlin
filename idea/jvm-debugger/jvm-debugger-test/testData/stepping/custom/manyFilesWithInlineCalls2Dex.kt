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