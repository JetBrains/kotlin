// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// FILE: JavaAnnotation.java
public @interface JavaAnnotation { }

// FILE: JavaParameterAnnotation.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
public @interface JavaParameterAnnotation { }

// FILE: JavaTypeAnnotation.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface JavaTypeAnnotation { }

// FILE: test.kt
context(a: String)
@get:JavaAnnotation val b: String
    get() = ""

context(a: String)
fun @receiver:JavaAnnotation String.b() { }

context(@JavaParameterAnnotation a: String)
fun foo() {}

context(a: @JavaTypeAnnotation String)
fun bar() {}