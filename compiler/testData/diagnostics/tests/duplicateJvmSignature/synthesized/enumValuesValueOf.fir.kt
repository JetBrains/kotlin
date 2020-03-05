enum class A {
    A1,
    A2;

    fun valueOf(s: String): A = valueOf(s)

    fun valueOf() = "OK"

    fun values(): Array<A> = null!!

    fun values(x: String) = x
}
