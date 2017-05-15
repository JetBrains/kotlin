fun f() {
    for (c in "123") {
        print(c)
    }
}

// 1 ISTORE 0\s+L3
// 1 ILOAD 0\s+INVOKEVIRTUAL java/io/PrintStream.print \(C\)V
// 1 LOCALVARIABLE c C L3 L6 0
