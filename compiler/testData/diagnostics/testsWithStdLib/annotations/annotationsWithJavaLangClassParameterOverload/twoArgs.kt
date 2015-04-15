// FILE: A.java
public @interface A {
    Class<?> arg1();
    Class<?> arg2();
}

// FILE: b.kt
A(arg1 = javaClass<String>(), arg2 = javaClass<Int>()) class MyClass1

A(arg1 = <!TYPE_MISMATCH!>javaClass<String>()<!>, arg2 = Int::class) class MyClass3
A(arg1 = String::class, arg2 = <!TYPE_MISMATCH!>javaClass<Int>()<!>) class MyClass4
