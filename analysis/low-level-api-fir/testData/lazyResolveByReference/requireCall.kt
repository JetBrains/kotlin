// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
@file:OptIn(ExperimentalContracts::class)
package library

import kotlin.contracts.*

class FunctionWithContract() {
    public inline fun require(value: Boolean): Unit {
        contract {
            returns() implies value
        }
        if (!value) {
            throw IllegalArgumentException()
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.FunctionWithContract

fun usage() {
    FunctionWithContract().re<caret>quire(true)
}