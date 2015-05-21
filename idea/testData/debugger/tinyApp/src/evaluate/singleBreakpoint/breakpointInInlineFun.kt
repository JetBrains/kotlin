package breakpointInInlineFun

import customLib.inlineFunInLibrary.*

fun main(args: Array<String>) {
    inlineFun { }
}

// ADDITIONAL_BREAKPOINT: inlineFunInLibrary.kt:public inline fun inlineFun