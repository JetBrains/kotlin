// FILE: A.java
public @interface A {
    Class<?>[] value();
}

// FILE: b.kt
@A(String::class, Int::class) class MyClass1
@A(*arrayOf(String::class, Int::class)) class MyClass2
@A(value = *arrayOf(String::class, Int::class)) class MyClass3
