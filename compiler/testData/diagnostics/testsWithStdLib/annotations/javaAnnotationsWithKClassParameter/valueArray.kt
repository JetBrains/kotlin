// FILE: A.java
public @interface A {
    Class<?>[] value();
}

// FILE: b.kt
A(String::class, Int::class) class MyClass1
A(*array(String::class, Int::class)) class MyClass2
A(value = *array(String::class, Int::class)) class MyClass3
