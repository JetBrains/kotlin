// "Create class 'Foo'" "true"

class B<T>(val t: T) {
    class Foo<U, V>(u: U, v: V) {

    }

}

class A<T>(val b: B<T>) {
    fun test() = B.Foo<Int, String>(2, "2")
}