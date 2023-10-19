abstract class A<T> {
    fun foo(x: T) {} // (1)
}

class B : A<String>() {
    // fake-override fun foo(x: String) // (2) should use 'B' class source
}
