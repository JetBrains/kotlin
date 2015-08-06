// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass): String {
    return javaClass.doSomething("") { it }
}

// FILE: JavaClass.java
public class JavaClass {
    public <T> T doSomething(T t, I<T> i) { return i.run(t); }
}

interface I<T> {
    T run(T t);
}