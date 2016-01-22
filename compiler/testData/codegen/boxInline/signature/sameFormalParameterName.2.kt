package test

class B<T>

interface A {
    fun <T> aTest(p: T): B<T>
}

open class Test {

    inline fun <T> test(crossinline z: () -> Int) = object : A {
        override fun <T> aTest(p: T): B<T> {
            z()
            return B<T>()
        }
    }

    fun callInline() =  test<String> { 1 }
}