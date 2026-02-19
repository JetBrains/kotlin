// TYPE_PARAMETER_TYPE: test/JavaClass#T

// FILE: test/JavaClass.java
package test;

public class JavaClass<T> {
    public T get() {
        return null;
    }
}

// FILE: main.kt
package test

fun test(obj: JavaClass<String>) {
    obj.get()
}