// FILE: A.java
public @interface A {
    Class<?> value() default Integer.class;
}

// FILE: b.kt
A(javaClass<String>()) class MyClass1

A(value = javaClass<String>()) class MyClass2

A class MyClass3
