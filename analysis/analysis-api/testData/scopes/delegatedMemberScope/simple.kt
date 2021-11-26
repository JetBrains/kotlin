// DO_NOT_CHECK_SYMBOL_RESTORE

interface I {
    fun foo()
}

class A(
    private val p: I
) : I by p

// class: A

