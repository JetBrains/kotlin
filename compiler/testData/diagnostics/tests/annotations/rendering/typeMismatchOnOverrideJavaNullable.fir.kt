// RENDER_DIAGNOSTICS_MESSAGES
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
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("foo; '@NotNull() @An() @MyTypeQualifier() fun foo(): @An() String' defined in '/A'")!>String?<!> = null
}

@An
public interface C {
    @NotNull
    @An
    fun foo(): String
}

class D : C {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("foo; '@NotNull() @An() fun foo(): String' defined in '/C'")!>String?<!> = null
}
