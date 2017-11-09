interface Test {
    fun invoke()
    operator fun invoke(i: Int): Int
}

operator fun Test.invoke() {}
operator fun Test.<!EXTENSION_SHADOWED_BY_MEMBER!>invoke<!>(i: Int) = i