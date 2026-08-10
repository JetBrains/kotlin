// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nestedClass */
