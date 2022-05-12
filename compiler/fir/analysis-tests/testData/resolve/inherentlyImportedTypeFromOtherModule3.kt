// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// MODULE: lib1
package lib1

abstract class FirstBase {
    interface Result
}

abstract class SecondBase : FirstBase()

// MODULE: lib2(lib1)
package lib2

import lib1.SecondBase

abstract class Test : SecondBase() {
    class Success : Result
}

// MODULE: main(lib1, lib2)
package main

import lib2.Test

class Main : Test() {
    fun usage(): Result {
        return Success()
    }
}