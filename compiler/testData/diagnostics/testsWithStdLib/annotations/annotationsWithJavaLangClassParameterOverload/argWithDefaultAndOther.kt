// !DIAGNOSTICS: -JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION
// FILE: A.java
public @interface A {
    Class<?> arg() default Integer.class;
    int x();
}

// FILE: b.kt
A(arg = javaClass<String>(), x = 3) class MyClass1
A(x = 5) class MyClass2
