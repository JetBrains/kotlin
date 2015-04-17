// !DIAGNOSTICS: -JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION
// FILE: A.java
public @interface A {
    Class<?> arg();
}

// FILE: b.kt
A(arg = javaClass<String>()) class MyClass
