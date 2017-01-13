interface Test {
    fun invoke()
}

operator fun Test.invoke() {}