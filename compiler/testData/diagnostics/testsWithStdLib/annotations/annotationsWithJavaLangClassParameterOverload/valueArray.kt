// FILE: A.java
public @interface A {
    Class<?>[] value();
}

// FILE: b.kt
A(javaClass<String>(), javaClass<Int>()) class MyClass1

A(*array(javaClass<String>(), javaClass<Int>())) class MyClass2

A(value = *array(javaClass<String>(), javaClass<Int>())) class MyClass3

A(<!TYPE_MISMATCH!>javaClass<String>()<!>, Int::class) class MyClass4
A(String::class, <!TYPE_MISMATCH!>javaClass<Int>()<!>) class MyClass5
