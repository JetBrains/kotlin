interface G<T> {
    fun foo()
    val bar: Int
}

fun <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>.foo() {}
val <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>.bar: Int get() = 42