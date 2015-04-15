// FILE: A.java
public @interface A {
    Class<?> arg();
}

// FILE: b.kt
A(arg = javaClass<String>()) class MyClass
