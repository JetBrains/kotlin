// FIR_IDENTICAL

// FILE: JavaClass.java

@FunctionalInterface
public interface JavaClass<T> {
    public T invoke();
}

// FILE: main.kt

fun main() {
    JavaClass {
        if (true) {
            return@JavaClass
        } else {
            return@JavaClass
        }
    }
}