// "Change type argument to String" "true"
abstract class Foo<T1, T2> {
    abstract fun foo1(): T1
    abstract val foo2: T2
}

interface Bar<T1, T2> {
    val bar1: T1
    fun bar2(): T2
}

class Test : Foo<Int, Int>(), Bar<Int, Int> {
    override fun foo1(): Int = 1
    override val foo2: Int = 2

    override val bar1: Int = 3
    override fun <caret>bar2() = "4"
}