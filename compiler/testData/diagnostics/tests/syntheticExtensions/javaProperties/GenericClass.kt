// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<String>) {
    javaClass.something += "x"
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public T getSomething() { return null; }
    public void setSomething(T value) { }
}