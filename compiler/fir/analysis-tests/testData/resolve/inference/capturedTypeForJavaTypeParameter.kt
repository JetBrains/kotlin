// FILE: B.java
public class B<E extends A> {}

// FILE: main.kt
interface A {}

fun <D : A> foo(b: B<D>) {}

fun main(b: B<*>) {
    foo(b)
}
