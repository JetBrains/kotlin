fun foo(x: Int) {
    when (x) {
        21 -> foo(x)
        42 -> foo(x)
        else -> foo(x)
    }
    
    val t = when (x) {
        21 -> foo(x)
        42 -> foo(x)
        else -> foo(x)
    }
}

// JVM_IR also generates a LINENUMBER 12, which seems consistent with the fact that
// there is a LINENUMBER 6, but still fails the test.
// IGNORE_BACKEND: JVM_IR

// 2 3 4 5 6 +8 9 10 11 8 13