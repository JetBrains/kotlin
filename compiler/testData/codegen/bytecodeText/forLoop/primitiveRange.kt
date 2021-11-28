// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun consume(i: Int) {}

fun f(r: IntRange) {
    for (i in r) {
        consume(i)
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getFirst
// 1 getLast
// 0 getStep

// 2 ISTORE
// 5 ILOAD
// 1 IINC
// 1 IF_ICMPGT
// 1 IF_ICMPEQ
// 2 IF