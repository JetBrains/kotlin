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
// 1 ISTORE 2\s+L5
// 1 ILOAD 2\s+INVOKEVIRTUAL java/io/PrintStream.print \(C\)V
// 1 LOCALVARIABLE c C L5 L\d+ 2