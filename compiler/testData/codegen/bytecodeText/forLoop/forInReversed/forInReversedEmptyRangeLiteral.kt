// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for (i in (4 .. 1).reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    for (i in (4L .. 1L).reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    for (i in ('D' .. 'A').reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
