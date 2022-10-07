// DO_NOT_CHECK_SYMBOL_RESTORE_K1

interface I {
    var Int.zoo: Unit
    fun foo()
    fun Int.smth(): Short
    val foo: Int
    var bar: Long
    val Int.doo: String
}

class A(
    private val p: I
) : I by p

// class: A

