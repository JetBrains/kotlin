// FILE: A.java
public @interface A {
    Class<?> arg();
}

// FILE: b.kt
@A(arg = String::class) class MyClass3
