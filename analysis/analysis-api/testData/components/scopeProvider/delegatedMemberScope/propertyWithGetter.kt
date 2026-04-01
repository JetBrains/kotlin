// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

interface I {
    val foo: Int get() = 2
}

class A(
    private val p: I
) : I by p

// class: A

