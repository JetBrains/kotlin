trait A<T, U> {
    fun foo(t: T, u: U) = "OK"
}

class B<T> : A<T, Int>

fun box(): String = B<Int>().foo(1, 2)
