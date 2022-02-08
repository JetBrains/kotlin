// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun f() {
    for (c in "123") {
        print(c)
    }
}

// Different locals slot numbers are used in JVM vs JVM IR for loop and induction variables.

// JVM_TEMPLATES
// 1 ISTORE 0\s+L3
// 1 ILOAD 0\s+INVOKEVIRTUAL java/io/PrintStream.print \(C\)V
// 1 LOCALVARIABLE c C L3 L\d+ 0

// JVM_IR_TEMPLATES
// 1 ISTORE 2\s+L4
// 1 ILOAD 2\s+INVOKEVIRTUAL java/io/PrintStream.print \(C\)V
// 1 LOCALVARIABLE c C L4 L\d+ 2