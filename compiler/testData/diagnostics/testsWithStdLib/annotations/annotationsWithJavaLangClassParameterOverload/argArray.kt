// !DIAGNOSTICS: -JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION
// FILE: A.java
public @interface A {
    Class<?>[] arg();
}

// FILE: b.kt

A(arg = arrayOf(javaClass<String>(), javaClass<Int>())) class MyClass1

A(arg = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf<!>(javaClass<String>(), Int::class)) class MyClass2
A(arg = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf<!>(String::class, javaClass<Int>())) class MyClass3
