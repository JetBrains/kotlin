// DO_NOT_CHECK_SYMBOL_RESTORE_K1

interface I {
    val foo: Int get() = 2
}

class A(
    private val p: I
) : I by p

// class: A

