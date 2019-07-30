@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package foo

expect interface A {
    fun commonFun()
}

class CommonGen<T : A> {
    val a: T get() = null!!
}

class List<out T>(val value: T)

fun getList(): List<A> = null!!