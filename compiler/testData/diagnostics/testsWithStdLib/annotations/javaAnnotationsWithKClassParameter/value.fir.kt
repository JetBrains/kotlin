// FILE: A.java
public @interface A {
    Class<?> value();
}

// FILE: b.kt
@A(String::class) class MyClass1
@A(value = String::class) class MyClass2
