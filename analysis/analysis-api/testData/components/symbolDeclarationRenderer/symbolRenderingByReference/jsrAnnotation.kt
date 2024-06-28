// MODULE: anno
// MODULE_KIND: LibraryBinary

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

public @interface Nonnull {}

// FILE: javax/annotation/meta/TypeQualifierDefault.java
package javax.annotation.meta;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeQualifierDefault {
    ElementType[] value() default {};
}


// MODULE: lib(anno)
// MODULE_KIND: LibraryBinary

// FILE: lib/EverythingIsNotNull.java
package lib;

import java.lang.annotation.ElementType;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Nonnull
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface EverythingIsNotNull {}

// FILE: lib/Lib.java
package lib;

import java.lang.annotation.Annotation;

@EverythingIsNotNull
public class Lib {
    public void test(Annotation[] annotations) {}
}


// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Lib

fun test(lib: Lib) {
    lib.t<caret><caret_onAirContext>est(null)
}