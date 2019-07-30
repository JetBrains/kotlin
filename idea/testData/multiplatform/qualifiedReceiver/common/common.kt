@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package foo

expect interface A {
    fun commonFun()
    val b: B
    fun bFun(): B
}

expect interface B {
    fun commonFunB()
}

class Common {
    val a: A get() = null!!
    fun aFun(): A = null!!
}
