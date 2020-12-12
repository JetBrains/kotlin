fun <T> test(): String {
    val x = object {
        fun <S> foo() = "OK"
    }
    return x.foo<Any>()
}

fun box() = test<Int>()