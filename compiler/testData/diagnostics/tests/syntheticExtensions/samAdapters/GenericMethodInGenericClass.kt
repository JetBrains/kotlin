// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<Int>): String {
    return javaClass.doSomething("", 1) { it }
}

// FILE: JavaClass.java
public class JavaClass<X> {
    public <T> T doSomething(T t, X x, I<T> i) { return i.run(t); }
}

interface I<T> {
    T run(T t);
}