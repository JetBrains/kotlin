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
A(arg = javaClass<String>(), b = B()) class MyClass1

A(b = B(y = 3)) class MyClass3

A(b = B(arg = javaClass<Double>())) class MyClass4

A(arg = javaClass<Boolean>(), b = B(arg = Char::class)) class MyClass5

A(arg = String::class, b = B(arg = javaClass<Boolean>())) class MyClass6
