// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: `@[FlexibleNullability]` differences are usually ignored, but its symbol is unbound in main's inlined copy, so it's not filtered out.
// MODULE: lib
// FILE: lib.kt
package lib

inline fun standardOutput(): Any = System.out

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.standardOutput

fun test(): Any = standardOutput()
