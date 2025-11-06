// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: Foo.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Foo {
    int a();
}

// FILE: b.kt
fun foo(): @Foo(<!SYNTAX!><!SYNTAX!><!>=<!> String = ""

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaType */
