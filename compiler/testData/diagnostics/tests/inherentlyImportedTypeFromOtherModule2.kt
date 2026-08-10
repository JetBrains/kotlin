// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nestedClass */
