// FIR_IDENTICAL
fun foo() {
    val a: Int
    try {
        a = 42
    } catch (e: Exception) {
    }
    finally {
    }
    <!UNINITIALIZED_VARIABLE!>a<!>.hashCode()
}