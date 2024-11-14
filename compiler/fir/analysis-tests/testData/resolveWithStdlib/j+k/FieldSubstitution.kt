// RUN_PIPELINE_TILL: BACKEND
// DISABLE_JAVA_FACADE
// FILE: JavaClass.java

public class JavaClass<T> {
    protected final T myHost;
}

// FILE: Derived.kt

public class Derived : JavaClass<String>() {
    fun test() {
        myHost.length
    }
}