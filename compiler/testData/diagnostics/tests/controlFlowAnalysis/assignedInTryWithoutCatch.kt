// FIR_IDENTICAL
fun foo() {
    val a: Int
    try {
        a = 42
    }
    finally {
    }
    a.hashCode()
}