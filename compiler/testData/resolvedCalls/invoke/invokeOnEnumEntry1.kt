enum class A {
    ONE, TWO

    fun invoke(i: Int) = i
}

fun test() = A.<caret>ONE(1)