// LANGUAGE: +ContractSyntaxV2
// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// WITH_STDLIB

// MODULE: lib
package lib

import kotlin.contracts.*

fun requireIsTrue(value: Boolean) contract [
    returns() implies value
] {
    if (!value) throw IllegalArgumentException()
}

// MODULE: main(lib)
package main

import lib.requireIsTrue

fun test(s: Any) {
    requireIsTrue(s is String)
    s.length
}
