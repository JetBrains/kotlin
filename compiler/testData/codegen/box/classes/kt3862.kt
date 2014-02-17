open class A<T> {
    open fun foo(a: T): Int = 2
}

trait B<T> : A<T> {
    override fun foo(a: T): Int = 1
}

class D : B<Int>, A<Int>() {
    fun boo(): Int {
        return super<B>.foo(1)
    }
}

fun box(): String {
    if (D().boo() != 1) return "Fail"
    return "OK"
}
