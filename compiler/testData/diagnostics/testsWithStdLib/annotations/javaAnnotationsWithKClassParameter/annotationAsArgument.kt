// FILE: A.java
public @interface A {
    Class<?> arg() default Integer.class;
    int x() default 1;
    B b();
}

// FILE: B.java
public @interface B {
    Class<?> arg() default String.class;
    int y() default 2;
}

// FILE: c.kt
@A(arg = String::class, b = B(y = 1)) class MyClass1

@A(b = B(y = 3)) class MyClass2

@A(arg = String::class, b = B(arg = Boolean::class)) class MyClass3
