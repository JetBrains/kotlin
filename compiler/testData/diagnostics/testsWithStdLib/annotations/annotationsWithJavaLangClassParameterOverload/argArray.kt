// FILE: A.java
public @interface A {
    Class<?>[] arg();
}

// FILE: b.kt

A(arg = array(javaClass<String>(), javaClass<Int>())) class MyClass1

A(arg = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>array<!>(javaClass<String>(), Int::class)) class MyClass2
A(arg = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>array<!>(String::class, javaClass<Int>())) class MyClass3
