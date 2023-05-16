// DO_NOT_CHECK_SYMBOL_RESTORE_K1

interface I {
    fun foo() = 4
    fun bar(): Int = 42
}

class A(
    private val p: I
) : I by p

// class: A

