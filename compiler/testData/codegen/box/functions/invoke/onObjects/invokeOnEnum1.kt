enum class A {
    ONE
    TWO

    fun invoke(i: Int) = i
}

fun box() = if (A.ONE(42) == 42) "OK" else "fail"
