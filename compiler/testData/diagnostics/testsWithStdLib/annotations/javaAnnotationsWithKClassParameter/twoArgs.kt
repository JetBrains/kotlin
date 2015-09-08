// FILE: A.java
public @interface A {
    Class<?> arg1();
    Class<?> arg2();
}

// FILE: b.kt
@A(arg1 = String::class, arg2 = Int::class) class MyClass
