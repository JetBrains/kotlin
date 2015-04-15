// FILE: A.java
public @interface A {
    Class<?>[] arg();
}

// FILE: b.kt
A(arg = array(String::class, Int::class)) class MyClass
