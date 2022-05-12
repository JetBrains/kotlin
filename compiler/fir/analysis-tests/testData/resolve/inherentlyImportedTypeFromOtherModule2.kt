// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// MODULE: lib
package lib

abstract class FirstBase {
    interface Result
}

abstract class SecondBase : FirstBase()

abstract class Test : SecondBase() {
    class Success : Result
}

// MODULE: main(lib)
package main

import lib.Test

class Main : Test() {
    fun usage(): Result {
        return Success()
    }
}