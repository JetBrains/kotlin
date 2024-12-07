fun test(b: Boolean): Int {
    val n: Int = <expr>(b.hashCode)</expr>()
    return n * 2
}

// Different behavior on invalid program between FE1.0 and FIR