package test

open class A<T> {
    open fun bar() {
    }
}

class B<T> : A<T>() {
    override fun bar() {
        super.bar()
    }
}