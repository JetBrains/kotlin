package inlineFunSameFile

import inlineFunPackage.*

fun box() {
    foo {
        println()
    }
}

// SMAP_APPLIED
// FILE: inlineFunctionFile.kt
// LINE: 4