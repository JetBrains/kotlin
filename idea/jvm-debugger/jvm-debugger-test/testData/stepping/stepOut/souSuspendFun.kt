package souSuspendFun

import forTests.builder

private fun foo(): Int {
    //Breakpoint!
    return 42                                                      // 1
}

// One line suspend wihtout doResume
suspend fun fourth() = foo()                                       // 2

// Multiline suspend without doResume
suspend fun third() : Int? {
    return fourth()                                                // 3
}

// One line suspend with doResume
suspend fun second(): Int = third()?.let { return it } ?: 0        // 4 (FIX IT)

// Multiline suspend with doResume
suspend fun first(): Int {
    second()                                                       // 5
    return 12
}

fun main(args: Array<String>) {
    builder {
        first()                                                    // 6
    }
}

// STEP_OUT: 5
