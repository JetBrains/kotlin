interface G<T> {
    fun foo()
    val bar: Int
}

fun <!OTHER_ERROR!>G<!>.foo() {}
val <!OTHER_ERROR!>G<!>.bar: Int get() = 42