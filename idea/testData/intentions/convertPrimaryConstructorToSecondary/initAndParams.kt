class InitAndParams<caret>(x: Int, z: Int) {
    val y = x

    val w: Int

    init {
        w = foo(y)
    }

    fun foo(arg: Int) = arg

    val v = w + z
}