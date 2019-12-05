class A<T>(t: T) {
    fun foo(x: T) {}
}

abstract class B<E>(e: E) {
    val myE: E = id(e)
    val a = A(e)

    fun id(e: E): E = e
}

class C : B<String>("") {
    fun bar() {
        a.foo("")
    }
}
