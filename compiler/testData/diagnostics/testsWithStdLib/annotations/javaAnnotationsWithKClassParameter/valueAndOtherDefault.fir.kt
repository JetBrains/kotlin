// FILE: A.java
public @interface A {
    Class<?> value();
    int x() default 1;
}

// FILE: b.kt
@A(String::class) class MyClass1
@A(value = String::class) class MyClass2

@A(String::class, x = 2) class MyClass3
@A(value = String::class, x = 4) class MyClass4
