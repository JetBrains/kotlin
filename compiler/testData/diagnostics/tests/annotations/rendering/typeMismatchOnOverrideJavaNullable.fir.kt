// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// SKIP_JAVAC

// FILE: MyTypeQualifier.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@javax.annotation.meta.TypeQualifier
public @interface MyTypeQualifier {}

// FILE: A.java

import org.jetbrains.annotations.NotNull;

@An
public interface A {
    @NotNull
    @An
    @MyTypeQualifier
    String foo();
}

// FILE: k.kt

import org.jetbrains.annotations.NotNull;

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

class B : A {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("fun foo(): String?; 'fun foo(): String' defined in 'A'")!>String?<!> = null
}

@An
public interface C {
    @NotNull
    @An
    fun foo(): String
}

class D : C {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("fun foo(): String?; 'fun foo(): String' defined in 'C'")!>String?<!> = null
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, interfaceDeclaration, javaType,
nullableType, override */
