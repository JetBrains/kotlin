// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

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
