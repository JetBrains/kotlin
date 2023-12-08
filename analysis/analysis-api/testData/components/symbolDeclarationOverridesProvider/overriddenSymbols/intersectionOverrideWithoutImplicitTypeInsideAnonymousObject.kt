package second

class MyClass {
    val prop = object : B<String> {
        override fun foo<caret>(x: String): Unit = Unit
    }
}

// FILE: B.kt
interface B<T>: C<T>, D<T>

// FILE: C.kt
interface C<T> {
    fun foo(x: T) {}
}

// FILE: D.kt
interface D<F> {
    fun foo(x: F) {}
}