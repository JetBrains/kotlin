// FILE: JavaClass.java
public class JavaClass<T> {
    public JavaClass(T t) {}

    public T foo() {}
}

// FILE: main.kt
fun main() {
    JavaClass<String>(null).foo().length
}
