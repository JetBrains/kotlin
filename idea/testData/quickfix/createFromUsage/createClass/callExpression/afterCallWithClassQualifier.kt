// "Create class 'Foo'" "true"

class A<T>(val n: T) {
    class Foo(i: Int) {

    }

}

fun test() {
    val a = A.Foo(2)
}