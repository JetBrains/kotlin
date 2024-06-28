package one

interface B<T> {
    fun f() = true
}

open class A<T>(b: Boolean)

interface D

class C : B<Int> {
    fun te<caret>st() {
        class LocalClass : A<D>(super<B>.f())
    }
}
