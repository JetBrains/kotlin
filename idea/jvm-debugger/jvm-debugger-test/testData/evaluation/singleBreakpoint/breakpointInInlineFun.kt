package breakpointInInlineFun

import customLib.inlineFunInLibrary.*

fun main(args: Array<String>) {
    inlineFun { }
}

// RESUME: 2

// ADDITIONAL_BREAKPOINT: inlineFunInLibrary.kt:public inline fun inlineFun
// ADDITIONAL_BREAKPOINT: inlineFunInLibrary.kt: Breakpoint 2