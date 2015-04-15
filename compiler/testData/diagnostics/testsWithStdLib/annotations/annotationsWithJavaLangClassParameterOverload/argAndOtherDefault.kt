// FILE: A.java
public @interface A {
    Class<?> arg();
    int x() default 1;
}

// FILE: b.kt
A(arg = javaClass<String>()) class MyClass1

A(arg = javaClass<String>(), x = 1) class MyClass2
