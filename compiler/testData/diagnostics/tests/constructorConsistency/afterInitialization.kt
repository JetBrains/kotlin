class My(val x: Int) {
    val y: Int = x + 3
    val z: Int? = foo()

    fun foo() = if (x >= 0) x else if (y >= 0) y else null
}