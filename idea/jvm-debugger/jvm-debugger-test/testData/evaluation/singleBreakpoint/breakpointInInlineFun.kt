// FILE: breakpointInInlineFun.kt
package breakpointInInlineFun

import customLib.inlineFunInLibrary.*

fun main(args: Array<String>) {
    inlineFun { }
}

// RESUME: 2

// ADDITIONAL_BREAKPOINT: inlineFunInLibrary.kt / public inline fun inlineFun
// ADDITIONAL_BREAKPOINT: inlineFunInLibrary.kt / Breakpoint 2

// FILE: customLib/inlineFunInLibrary/inlineFunInLibrary.kt
package customLib.inlineFunInLibrary

public inline fun inlineFun(f: () -> Unit) {
    1 + 1
    inlineFunInner {
        1 + 1
    }
}

public inline fun inlineFunInner(f: () -> Unit) {
    // Breakpoint 2
    1 + 1
}