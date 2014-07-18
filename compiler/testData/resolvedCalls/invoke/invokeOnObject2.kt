object A {
    fun invoke(i: Int) = i
}

fun test() = A<caret>(1)