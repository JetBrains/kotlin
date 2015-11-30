package inlineFunFromLibrary

import inlineFunInLibrary.*

fun box() {
    foo {
        println()
    }
}

// WITH_MOCK_LIBRARY: true
// FILE: inlineFunInLibrary.kt
// LINE: 4