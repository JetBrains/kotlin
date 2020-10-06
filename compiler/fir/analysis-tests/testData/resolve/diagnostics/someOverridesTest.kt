open class A
open class B : A()

open class First<T> {
    open fun test(item: T) {}
}

open class Second : First<A>() {
    override fun test(item: A) {}
}
