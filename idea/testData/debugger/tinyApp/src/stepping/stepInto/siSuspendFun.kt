package siSuspendFun

import forTests.builder

private fun foo(): Int {
    return 42                                                      // 6
}

// One line suspend wihtout doResume
suspend fun fourth() = foo()                                       // 5

// Multiline suspend without doResume
suspend fun third() : Int? {
    return fourth()                                                // 4
}

// One line suspend with doResume
suspend fun second(): Int = third()?.let { return it } ?: 0        // 3

// Multiline suspend with doResume
suspend fun first(): Int {                                         //
    second()                                                       // 2
    return 12
}

fun main(args: Array<String>) {
    builder {
        //Breakpoint!
        first()                                                    // 1
        foo()
    }
}

// STEP_INTO: 5
