// FILE: A.java
public @interface A {
Class<?> value() default Integer.class;
int x();
}

// FILE: b.kt
A(javaClass<String>(), x = 1) class MyClass1
A(value = javaClass<String>(), x = 3) class MyClass2
A(x = 5) class MyClass3
