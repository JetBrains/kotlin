// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-52407

// FILE: x.kt

package x

class Throws {
    fun test() {}
}

// FILE: main.kt

import x.*

fun main() {
    Throws().test()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
