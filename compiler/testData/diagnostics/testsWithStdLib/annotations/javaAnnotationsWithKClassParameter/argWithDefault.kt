// FILE: A.java
public @interface A {
    Class<?> arg() default Integer.class;
}

// FILE: b.kt
@A(arg = String::class) class MyClass1
@A class MyClass2
