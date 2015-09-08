// FILE: A.java
public @interface A {
    Class<?> value() default Integer.class;
    int x();
}

// FILE: b.kt
@A(String::class, x = 2) class MyClass1
@A(value = String::class, x = 4) class MyClass2
@A(x = 5) class MyClass3
