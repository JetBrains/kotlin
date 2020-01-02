// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<String>) {
    javaClass.<!VARIABLE_EXPECTED!>something<!> += "x"
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public T getSomething() { return null; }
    public void setSomething(T value) { }
}