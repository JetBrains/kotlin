// FILE: A.java
public @interface A {
    Class<?> value() default Integer.class;
}

// FILE: b.kt
@A(String::class) class MyClass1
@A(value = String::class) class MyClass2
@A class MyClass3
