// FILE: A.java
public @interface A {
    Class<?> arg() default Integer.class;
}

// FILE: b.kt
A(arg = javaClass<String>()) class MyClass1
A class MyClass3
