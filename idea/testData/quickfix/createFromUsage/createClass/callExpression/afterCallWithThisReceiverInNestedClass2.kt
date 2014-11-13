// "Create class 'Foo'" "true"

class A<T>(val n: T) {
    inner class Foo(i: Int, s: String) {

    }

    inner class B<U>(val m: U) {
        fun test() = this@A.Foo(2, "2")
    }
}