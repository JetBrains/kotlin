package dexSeveralInlineCallsFromOtherFile

fun main(args: Array<String>) {
    //Breakpoint!
    firstCall()
    secondCall()
}

fun firstCall() {
    inlineFun()
}

fun secondCall() {
    inlineFun()
}

// RESUME: 2
// ADDITIONAL_BREAKPOINT: dexSeveralInlineCallsFromOtherFile.Other.kt: Breakpoint 1
