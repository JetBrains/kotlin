// !CHECK_TYPE

// FILE: A.java
public class A<T> {
    public A<T>[] baz() { return null; }
}


// FILE: main.kt

fun foo1(x: A<*>) = x.baz()
fun foo2(x: A<*>) {
    x.baz() checkType { _<Array<out A<*>>>() }
}
