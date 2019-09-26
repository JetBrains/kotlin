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