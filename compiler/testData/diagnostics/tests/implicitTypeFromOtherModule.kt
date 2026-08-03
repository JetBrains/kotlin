// RUN_PIPELINE_TILL: BACKEND
// MODULE: lib
package dependency

class Other

class Lib

fun test() = Lib()

// MODULE: main(lib)
package main

import dependency.test
import dependency.Lib
import dependency.Other

fun usage() {
    take(test())
}

fun take(a: Lib) {}

fun take(a: Other) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
