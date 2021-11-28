fun f() = IntArray(1) { run { return@IntArray 1 } }

// On JVM_IR, the return is an assignment to a captured var followed by
// a non-local `break` from a `do ... while (false)`. The var should be optimized.
// 0 IntRef
