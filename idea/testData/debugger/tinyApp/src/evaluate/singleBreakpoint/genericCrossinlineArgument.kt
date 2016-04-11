package genericCrossinlineArgument

interface A {
    fun expr(): Boolean
}

inline fun <T> test1(t: T, crossinline formula: T.() -> Boolean) = object : A {
    override fun expr() = formula(t)
}

fun main(args: Array<String>) {
    test1("aaa") {
        //Breakpoint!
        1 + 1 > 0
    }.expr()
}