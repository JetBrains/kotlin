// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaClass.java
public class JavaClass<T> {
    private T it;

    public JavaClass(T t) {
        it = t;
    }

    public T foo() { return it; }
}

// FILE: main.kt
fun main() {
    JavaClass<String>(null).foo().length
}
