// "Create class 'Foo'" "true"

class A<T>(val n: T) {
    inner class Foo(i: Int, s: String) {

    }

    fun test() = this.Foo(2, "2")
}