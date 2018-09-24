package inlineFunctionBreakpointVariants

import inlineFunctionOtherPackage.*

fun main(args: Array<String>) {
    //Breakpoint! (lambdaOrdinal = -1)
    foo1 { foo2() }
}

inline fun foo1(f: () -> Unit) {
    f()
}

inline fun foo2() = 1

// RESUME: 1