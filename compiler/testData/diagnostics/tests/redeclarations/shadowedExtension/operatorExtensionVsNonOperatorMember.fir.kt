interface Test {
    fun invoke()
    operator fun invoke(i: Int): Int
}

operator fun Test.invoke() {}
operator fun Test.invoke(i: Int) = i