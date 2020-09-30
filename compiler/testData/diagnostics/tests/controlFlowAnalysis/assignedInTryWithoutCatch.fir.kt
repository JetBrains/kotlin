fun foo() {
    val a: Int
    try {
        a = 42
    }
    finally {
    }
    <!UNINITIALIZED_VARIABLE!>a<!>.hashCode()
}