// SKIP_JAVAC
// This directive is needed to skip this test in LazyBodyIsNotTouchedTilContractsPhaseTestGenerated,
//  because it fails to parse module structure of multimodule test

// MODULE: lib
package dependency

abstract class Test {
    interface Result

    class Success : Result
}


// MODULE: main(lib)
package main

import dependency.Test

class Main : Test() {
    fun usage(): Result {
        return Success()
    }
}