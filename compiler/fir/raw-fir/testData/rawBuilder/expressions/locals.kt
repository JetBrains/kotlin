fun withLocals(p: Int): Int {
    class Local(val pp: Int) {
        fun diff() = pp - p
    }

    val x = Local(42).diff()

    fun sum(y: Int, z: Int, f: (Int, Int) -> Int): Int {
        return x + f(y, z)
    }

    val code = (object : Any() {
        fun foo() = hashCode()
    }).foo()

    return sum(code, Local(1).diff(), fun(x: Int, y: Int) = x + y)
}